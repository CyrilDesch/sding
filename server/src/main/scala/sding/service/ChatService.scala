package sding.service

import cats.effect.Async
import cats.effect.Ref
import cats.effect.std.Queue
import cats.syntax.all.*
import chat4s.ai.prompt.PromptLoader
import chat4s.graph.WorkflowEngine
import chat4s.graph.WorkflowId
import org.typelevel.otel4s.trace.Tracer
import chat4s.ai.tools.WebSearchTool
import sding.domain.AppError
import sding.domain.ChatId
import sding.domain.UserId
import sding.protocol.ChatSummary
import sding.protocol.SseEvent
import sding.protocol.WorkflowStep
import sding.repository.ChatRepository
import sding.repository.ContentType
import sding.repository.MessageRecord
import sding.repository.MessageRepository
import sding.repository.ProjectRepository
import sding.repository.SenderType
import sding.repository.StepRepository
import sding.repository.UserRepository
import sding.workflow.graph.ProjectContextGraph
import sding.workflow.state.ProjectContextState

trait ChatService[F[_]]:
  def createChat(userId: UserId): F[ChatId]
  def submitInput(chatId: ChatId, input: String): F[Unit]
  def listChats(userId: UserId): F[List[ChatSummary]]
  def deleteChat(chatId: ChatId, userId: UserId): F[Unit]

  /** Stream live events starting from [afterIndex, ∞). Pass the `liveIndex`
    * returned by [[chatHistory]] to avoid re-delivering history events.
    */
  def eventStream(chatId: ChatId, afterIndex: Int): fs2.Stream[F, SseEvent]

  /** Returns all persisted events for the chat together with the current event
    * log size (`liveIndex`). Pass `liveIndex` to [[eventStream]] so SSE only
    * delivers events that were not already included in the history response.
    */
  def chatHistory(chatId: ChatId): F[(events: List[SseEvent], liveIndex: Int)]

final class LiveChatService[F[_]: Async: Tracer](
    userRepo: UserRepository[F],
    projectRepo: ProjectRepository[F],
    chatRepo: ChatRepository[F],
    stepRepo: StepRepository[F],
    messageRepo: MessageRepository[F],
    encryptionSvc: EncryptionService[F],
    promptLoader: PromptLoader[F],
    workflowEngine: WorkflowEngine[F],
    sessions: Ref[F, Map[ChatId, ChatSession[F]]]
) extends ChatService[F]:

  def createChat(userId: UserId): F[ChatId] =
    for
      project <- projectRepo.create(userId, "Brainstorming Session", "en")
      step    <- stepRepo.create(project.id, "project_context")
      chat    <- chatRepo.create(project.id, step.id, "Brainstorming Session")
      chatId = chat.id
      session <- startWorkflow(
        chatId,
        userId,
        step.id,
        ProjectContextState(chatIdStr = Some(chatId.asString))
      )
      _ <- sessions.update(_.updated(chatId, session))
    yield chatId

  def listChats(userId: UserId): F[List[ChatSummary]] =
    for
      projects  <- projectRepo.findByUser(userId)
      chatLists <- projects.traverse(p => chatRepo.findByProject(p.id))
    yield chatLists.flatten
      .sortBy(_.createdAt)(using Ordering[java.time.Instant].reverse)
      .map(c => ChatSummary(chatId = c.id.asString, title = c.title))

  def deleteChat(chatId: ChatId, userId: UserId): F[Unit] =
    for
      chat <- chatRepo.findById(chatId).flatMap {
        case None    => Async[F].raiseError(AppError.ChatError.ChatNotFound(chatId))
        case Some(c) => Async[F].pure(c)
      }
      project <- projectRepo.findById(chat.projectId).flatMap {
        case None    => Async[F].raiseError(AppError.ChatError.ChatNotFound(chatId))
        case Some(p) => Async[F].pure(p)
      }
      _ <-
        if project.userId != userId then
          Async[F].raiseError(AppError.AuthError.InsufficientPermissions("Cannot delete another user's chat"))
        else Async[F].unit
      _ <- projectRepo.softDelete(project.id)
      _ <- sessions.update(_ - chatId)
    yield ()

  def submitInput(chatId: ChatId, input: String): F[Unit] =
    sessions.get.flatMap { map =>
      map.get(chatId) match
        case Some(session) =>
          messageRepo.create(chatId, SenderType.User, input, ContentType.Text, None) *>
            session.inbound.offer(input)
        case None =>
          Async[F].raiseError(AppError.ChatError.ChatNotFound(chatId))
    }

  def eventStream(chatId: ChatId, afterIndex: Int): fs2.Stream[F, SseEvent] =
    fs2.Stream.eval(sessions.get).flatMap { map =>
      map.get(chatId) match
        case Some(session) =>
          session.eventLog.subscribeFrom(afterIndex)
        case None =>
          resumeFromDb(chatId)
    }

  def chatHistory(chatId: ChatId): F[(events: List[SseEvent], liveIndex: Int)] =
    chatRepo.findById(chatId).flatMap {
      case None    => Async[F].raiseError(AppError.ChatError.ChatNotFound(chatId))
      case Some(_) =>
        for
          messages <- messageRepo.findByChat(chatId, Int.MaxValue, 0)
          liveIdx  <- sessions.get.flatMap(map => map.get(chatId).fold(Async[F].pure(0))(_.eventLog.currentSize))
          plan   = SseEvent.WorkflowPlan(WorkflowStep.values.toList)
          events = messages.flatMap(messageToEvent)
        yield (events = plan :: events, liveIndex = liveIdx)
    }

  private def resumeFromDb(chatId: ChatId): fs2.Stream[F, SseEvent] =
    fs2.Stream.eval(chatRepo.findById(chatId)).flatMap {
      case None       => fs2.Stream.raiseError(AppError.ChatError.ChatNotFound(chatId))
      case Some(chat) =>
        chat.currentStepId match
          case None =>
            fs2.Stream.raiseError(AppError.ChatError.ChatNotFound(chatId))
          case Some(stepId) =>
            fs2.Stream.eval(stepRepo.findById(stepId)).flatMap {
              case None =>
                fs2.Stream.raiseError(AppError.ChatError.ChatNotFound(chatId))
              case Some(step) if step.isFinished =>
                fs2.Stream.emit(SseEvent.WorkflowComplete(chatId.asString))
              case Some(step) =>
                fs2.Stream.eval(resumeWorkflowFromStep(chatId, chat.projectId, step)).flatMap { session =>
                  session.eventLog.subscribeFrom(0)
                }
            }
    }

  private def resumeWorkflowFromStep(
      chatId: ChatId,
      projectId: sding.domain.ProjectId,
      step: sding.repository.StepRecord
  ): F[ChatSession[F]] =
    for
      projectOpt <- projectRepo.findById(projectId)
      project    <- projectOpt match
        case None    => Async[F].raiseError(AppError.ChatError.ChatNotFound(chatId))
        case Some(p) => Async[F].pure(p)
      llmCfg <- userRepo.getLlmConfig(project.userId).flatMap {
        case Some(cfg) => Async[F].pure(cfg)
        case None      => Async[F].raiseError(AppError.ChatError.LlmNotConfigured(project.userId))
      }
      (provider, encryptedKey, model) = llmCfg
      apiKey                   <- encryptionSvc.decrypt(encryptedKey)
      agent                    <- chat4s.ai.AgentFactory.makeAgent(provider, apiKey, model)
      (ctx, eventLog, inbound) <- LiveChatContext.make[F](messageRepo, chatId)
      wf      = ProjectContextGraph.build(agent, promptLoader, ctx, WebSearchTool.stub[F])
      journal = new StepRepoWorkflowJournal[F](stepRepo, step.id)
      wfId     = WorkflowId(step.id.value.toString)
      execOpt   <- workflowEngine.resume(wfId, wf, journal)
      execution <- execOpt match
        case Some(exec) => Async[F].pure(exec)
        case None       =>
          // Edge case: the step is in DB but no checkpoint is parseable; restart from entry
          workflowEngine.start(wfId, wf, ProjectContextState(chatIdStr = Some(chatId.asString)), journal)
      session = ChatSession(ctx, eventLog, inbound)
      _ <- sessions.update(_.updated(chatId, session))
      _ <- Async[F].start(runExecution(chatId, step.id, ctx, eventLog, execution))
    yield session

  private def startWorkflow(
      chatId: ChatId,
      userId: UserId,
      stepId: sding.domain.StepId,
      initialState: ProjectContextState
  ): F[ChatSession[F]] =
    for
      llmCfg <- userRepo.getLlmConfig(userId).flatMap {
        case Some(cfg) => Async[F].pure(cfg)
        case None      => Async[F].raiseError(AppError.ChatError.LlmNotConfigured(userId))
      }
      (provider, encryptedKey, model) = llmCfg
      apiKey                   <- encryptionSvc.decrypt(encryptedKey)
      agent                    <- chat4s.ai.AgentFactory.makeAgent(provider, apiKey, model)
      (ctx, eventLog, inbound) <- LiveChatContext.make[F](messageRepo, chatId)
      wf      = ProjectContextGraph.build(agent, promptLoader, ctx, WebSearchTool.stub[F])
      journal = new StepRepoWorkflowJournal[F](stepRepo, stepId)
      wfId     = WorkflowId(stepId.value.toString)
      execution <- workflowEngine.start(wfId, wf, initialState, journal)
      session = ChatSession(ctx, eventLog, inbound)
      _ <- Async[F].start(runExecution(chatId, stepId, ctx, eventLog, execution))
    yield session

  private def runExecution(
      chatId: ChatId,
      stepId: sding.domain.StepId,
      ctx: LiveChatContext[F],
      eventLog: EventLog[F],
      execution: chat4s.graph.WorkflowExecution[F, ProjectContextState]
  ): F[Unit] =
    val plan = WorkflowStep.values.toList
    (eventLog.publish(SseEvent.WorkflowPlan(plan)) *>
      execution.stream
        .evalTap { result =>
          WorkflowStep.fromString(result.stepId).fold(Async[F].unit) { step =>
            ctx.setCurrentNode(step) *>
              messageRepo.create(chatId, SenderType.System, "node_complete", ContentType.Text, Some(step.snakeName)) *>
              eventLog.publish(SseEvent.NodeComplete(step))
          }
        }
        .compile
        .drain
        .flatMap { _ =>
          messageRepo.create(chatId, SenderType.System, "workflow_complete", ContentType.Text, None) *>
            stepRepo.markFinished(stepId) *>
            eventLog.publish(SseEvent.WorkflowComplete(chatId.asString))
        })
      .handleErrorWith(e =>
        eventLog.publish(SseEvent.Error(e.getMessage)) *>
          sessions.update(_ - chatId)
      )

  private def messageToEvent(msg: MessageRecord): Option[SseEvent] =
    msg.senderType match
      case SenderType.Agent =>
        Some(
          SseEvent.Message(msg.content, msg.contentType.formatString, msg.sourceNode.flatMap(WorkflowStep.fromString))
        )
      case SenderType.User =>
        Some(SseEvent.UserMessage(msg.content))
      case SenderType.System =>
        msg.content match
          case "node_complete" =>
            msg.sourceNode.flatMap(WorkflowStep.fromString).map(SseEvent.NodeComplete.apply)
          case "workflow_complete" =>
            Some(SseEvent.WorkflowComplete(msg.chatId.asString))
          case encoded =>
            io.circe.parser.parse(encoded).toOption.flatMap { json =>
              val cursor     = json.hcursor
              val sourceNode = msg.sourceNode.flatMap(WorkflowStep.fromString)
              cursor.get[String]("type").toOption match
                case Some("selection_request") =>
                  for
                    title      <- cursor.get[String]("title").toOption
                    items      <- cursor.get[List[chat4s.io.SelectionItem]]("items").toOption
                    allowRetry <- cursor.get[Boolean]("allow_retry").toOption
                  yield SseEvent.SelectionRequest(title, items, allowRetry, sourceNode)
                case _ =>
                  for
                    prompt  <- cursor.get[String]("prompt").toOption
                    options <- cursor.get[Option[List[String]]]("options").toOption
                  yield SseEvent.InputRequest(prompt, options, sourceNode)
            }

final case class ChatSession[F[_]](
    ctx: LiveChatContext[F],
    eventLog: EventLog[F],
    inbound: Queue[F, String]
)

object LiveChatService:
  def make[F[_]: Async: Tracer](
      userRepo: UserRepository[F],
      projectRepo: ProjectRepository[F],
      chatRepo: ChatRepository[F],
      stepRepo: StepRepository[F],
      messageRepo: MessageRepository[F],
      encryptionSvc: EncryptionService[F],
      promptLoader: PromptLoader[F]
  ): F[ChatService[F]] =
    Ref.of[F, Map[ChatId, ChatSession[F]]](Map.empty).map { sessions =>
      new LiveChatService[F](
        userRepo,
        projectRepo,
        chatRepo,
        stepRepo,
        messageRepo,
        encryptionSvc,
        promptLoader,
        WorkflowEngine.make[F],
        sessions
      )
    }
