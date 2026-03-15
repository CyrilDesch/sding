package sding.service

import cats.effect.Async
import cats.effect.Ref
import cats.effect.std.Queue
import cats.syntax.all.*
import sding.agent.*
import sding.domain.AppError
import sding.domain.ChatId
import sding.domain.UserId
import sding.protocol.SseEvent
import sding.repository.UserRepository
import sding.workflow.graph.ProjectContextGraph
import sding.workflow.io.ChatContext
import sding.workflow.state.ProjectContextState
import sding.workflow.task.*

trait ChatService[F[_]]:
  def createChat(userId: UserId): F[ChatId]
  def submitInput(chatId: ChatId, input: String): F[Unit]
  def eventStream(chatId: ChatId): fs2.Stream[F, SseEvent]

final class LiveChatService[F[_]: Async](
    userRepo: UserRepository[F],
    encryptionSvc: EncryptionService[F],
    quotaManager: QuotaManager[F],
    promptLoader: PromptLoader[F],
    sessions: Ref[F, Map[ChatId, ChatSession[F]]]
) extends ChatService[F]:

  def createChat(userId: UserId): F[ChatId] =
    for
      llmCfg <- userRepo.getLlmConfig(userId).flatMap {
        case Some(cfg) => Async[F].pure(cfg)
        case None      => Async[F].raiseError(AppError.ChatError.LlmNotConfigured(userId))
      }
      (provider, encryptedKey, model) = llmCfg
      apiKey <- encryptionSvc.decrypt(encryptedKey)
      agent  <- AgentFactory.makeAgent(provider, apiKey, model, quotaManager)
      chatId = ChatId.random
      (ctx, outbound, inbound) <- LiveChatContext.make[F]
      tasks   = buildTaskMap(agent, promptLoader, ctx, WebSearchTool.stub[F])
      session = ChatSession(ctx, outbound, inbound)
      _ <- sessions.update(_.updated(chatId, session))
      _ <- Async[F].start(runWorkflow(chatId, ctx, outbound, tasks))
    yield chatId

  def submitInput(chatId: ChatId, input: String): F[Unit] =
    sessions.get.flatMap { map =>
      map.get(chatId) match
        case Some(session) => session.inbound.offer(input)
        case None          => Async[F].raiseError(AppError.ChatError.ChatNotFound(chatId))
    }

  def eventStream(chatId: ChatId): fs2.Stream[F, SseEvent] =
    fs2.Stream.eval(sessions.get).flatMap { map =>
      map.get(chatId) match
        case Some(session) =>
          fs2.Stream.fromQueueUnterminated(session.outbound)
        case None =>
          fs2.Stream.raiseError(AppError.ChatError.ChatNotFound(chatId))
    }

  private def buildTaskMap(
      agent: Agent[F],
      promptLoader: PromptLoader[F],
      chatCtx: ChatContext[F],
      searchTool: AgentTool[F]
  ): Map[String, TaskNode[F]] =
    Map(
      "human_requirements"            -> HumanRequirementsTask[F](chatCtx),
      "weird_problem_generation"      -> WeirdProblemGenerationTask[F](agent, promptLoader, chatCtx),
      "problem_reformulation"         -> ProblemReformulationTask[F](agent, promptLoader, chatCtx),
      "trend_analysis"                -> TrendAnalysisTask[F](agent, promptLoader, chatCtx, searchTool),
      "problem_selection"             -> ProblemSelectionTask[F](chatCtx),
      "user_interviews"               -> UserInterviewsTask[F](agent, promptLoader, chatCtx),
      "empathy_map"                   -> EmpathyMapTask[F](agent, promptLoader, chatCtx),
      "jtbd_definition"               -> JTBDDefinitionTask[F](agent, promptLoader, chatCtx),
      "hmw"                           -> HMWTask[F](agent, promptLoader, chatCtx),
      "crazy8s"                       -> Crazy8sTask[F](agent, promptLoader, chatCtx),
      "scamper"                       -> ScamperTask[F](agent, promptLoader, chatCtx),
      "competitive_analysis"          -> CompetitiveAnalysisTask[F](agent, promptLoader, chatCtx, searchTool),
      "prototype_builds"              -> PrototypeBuildsTask[F](agent, promptLoader, chatCtx),
      "premium_report"                -> PremiumReportTask[F](agent, promptLoader, chatCtx),
      "human_problem_selection"       -> HumanProblemSelectionTask[F](chatCtx),
      "human_jtbd_selection"          -> HumanJTBDSelectionTask[F](chatCtx),
      "human_comprehensive_selection" -> HumanComprehensiveSelectionTask[F](chatCtx),
      "human_project_selection"       -> HumanProjectSelectionTask[F](chatCtx),
      "markdown_generation"           -> MarkdownGenerationTask[F]()
    )

  private def runWorkflow(
      chatId: ChatId,
      ctx: LiveChatContext[F],
      outbound: Queue[F, SseEvent],
      tasks: Map[String, TaskNode[F]]
  ): F[Unit] =
    val graph        = ProjectContextGraph.build(tasks)
    val initialState = ProjectContextState(chatIdStr = Some(chatId.asString))
    ProjectContextGraph
      .execute(graph, initialState)
      .evalTap { case (nodeName, _) =>
        ctx.setCurrentNode(nodeName) *> outbound.offer(SseEvent.NodeComplete(nodeName))
      }
      .compile
      .drain
      .flatMap(_ => outbound.offer(SseEvent.WorkflowComplete(chatId.asString)))
      .handleErrorWith(e => outbound.offer(SseEvent.Error(e.getMessage)))

final case class ChatSession[F[_]](
    ctx: LiveChatContext[F],
    outbound: Queue[F, SseEvent],
    inbound: Queue[F, String]
)

object LiveChatService:
  def make[F[_]: Async](
      userRepo: UserRepository[F],
      encryptionSvc: EncryptionService[F],
      quotaManager: QuotaManager[F],
      promptLoader: PromptLoader[F]
  ): F[ChatService[F]] =
    Ref.of[F, Map[ChatId, ChatSession[F]]](Map.empty).map { sessions =>
      new LiveChatService[F](userRepo, encryptionSvc, quotaManager, promptLoader, sessions)
    }
