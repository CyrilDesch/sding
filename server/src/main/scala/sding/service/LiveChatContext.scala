package sding.service

import cats.effect.Async
import cats.effect.std.Queue
import cats.syntax.all.*
import sding.protocol.SseEvent
import sding.workflow.io.ChatContext
import sding.workflow.io.MessageFormat

final class LiveChatContext[F[_]: Async](
    outbound: Queue[F, SseEvent],
    inbound: Queue[F, String],
    nodeNameRef: cats.effect.Ref[F, Option[String]]
) extends ChatContext[F]:

  def sendMessage(message: String, format: MessageFormat): F[Unit] =
    nodeNameRef.get.flatMap { sn =>
      val fmt = format match
        case MessageFormat.Text     => "text"
        case MessageFormat.Html     => "html"
        case MessageFormat.Markdown => "markdown"
      outbound.offer(SseEvent.Message(message, fmt, sn))
    }

  def sendState(message: String): F[Unit] =
    nodeNameRef.get.flatMap(sn => outbound.offer(SseEvent.StateUpdate(message, sn)))

  def requestInput(prompt: String, options: Option[List[String]]): F[String] =
    nodeNameRef.get.flatMap { sn =>
      outbound.offer(SseEvent.InputRequest(prompt, options, sn)) *> inbound.take
    }

  def setCurrentNode(name: String): F[Unit] = nodeNameRef.set(Some(name))

object LiveChatContext:
  def make[F[_]: Async]: F[(LiveChatContext[F], Queue[F, SseEvent], Queue[F, String])] =
    for
      outbound    <- Queue.unbounded[F, SseEvent]
      inbound     <- Queue.unbounded[F, String]
      nodeNameRef <- cats.effect.Ref.of[F, Option[String]](None)
      ctx = new LiveChatContext[F](outbound, inbound, nodeNameRef)
    yield (ctx, outbound, inbound)
