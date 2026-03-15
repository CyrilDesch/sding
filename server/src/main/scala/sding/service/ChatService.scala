package sding.service

import cats.effect.Async
import cats.effect.Ref
import cats.effect.std.Queue
import cats.syntax.all.*
import sding.domain.AppError
import sding.domain.ChatId
import sding.protocol.SseEvent
import sding.workflow.graph.ProjectContextGraph
import sding.workflow.state.ProjectContextState
import sding.workflow.task.TaskNode

trait ChatService[F[_]]:
  def createChat: F[ChatId]
  def submitInput(chatId: ChatId, input: String): F[Unit]
  def eventStream(chatId: ChatId): fs2.Stream[F, SseEvent]

final class LiveChatService[F[_]: Async](
    tasks: Map[String, TaskNode[F]],
    sessions: Ref[F, Map[ChatId, ChatSession[F]]]
) extends ChatService[F]:

  def createChat: F[ChatId] =
    val chatId = ChatId.random
    for
      (ctx, outbound, inbound) <- LiveChatContext.make[F]
      session = ChatSession(ctx, outbound, inbound)
      _ <- sessions.update(_.updated(chatId, session))
      _ <- Async[F].start(runWorkflow(chatId, ctx, outbound))
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

  private def runWorkflow(chatId: ChatId, ctx: LiveChatContext[F], outbound: Queue[F, SseEvent]): F[Unit] =
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
  def make[F[_]: Async](tasks: Map[String, TaskNode[F]]): F[ChatService[F]] =
    Ref.of[F, Map[ChatId, ChatSession[F]]](Map.empty).map { sessions =>
      new LiveChatService[F](tasks, sessions)
    }
