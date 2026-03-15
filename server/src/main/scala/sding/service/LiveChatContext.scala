package sding.service

import cats.effect.Async
import cats.effect.Ref
import cats.effect.std.Queue
import cats.syntax.all.*
import io.circe.syntax.*
import sding.domain.ChatId
import sding.protocol.SelectionItem
import sding.protocol.SseEvent
import sding.protocol.WorkflowStep
import sding.repository.ContentType
import sding.repository.MessageRepository
import sding.repository.SenderType
import sding.workflow.io.ChatContext
import sding.workflow.io.MessageFormat
import sding.workflow.io.UserInputRequest

final class LiveChatContext[F[_]: Async](
    outbound: EventLog[F],
    inbound: Queue[F, String],
    nodeNameRef: Ref[F, Option[WorkflowStep]],
    messageRepo: MessageRepository[F],
    chatId: ChatId
) extends ChatContext[F]:

  def sendMessage(message: String, format: MessageFormat): F[Unit] =
    nodeNameRef.get.flatMap { sn =>
      val contentType = format match
        case MessageFormat.Text     => ContentType.Text
        case MessageFormat.Html     => ContentType.Html
        case MessageFormat.Markdown => ContentType.Markdown
      messageRepo.create(chatId, SenderType.Agent, message, contentType, sn.map(_.snakeName)) *>
        outbound.publish(SseEvent.Message(message, contentType.formatString, sn))
    }

  def sendState(message: String): F[Unit] =
    nodeNameRef.get.flatMap(sn => outbound.publish(SseEvent.StateUpdate(message, sn)))

  def requestInput(request: UserInputRequest): F[String] =
    nodeNameRef.get.flatMap { sn =>
      val (prompt, options) = request match
        case UserInputRequest.FreeText(p)     => (p, None)
        case UserInputRequest.Choice(p, opts) => (p, Some(opts))
      val optionsJson    = options.map(_.asJson.noSpaces).getOrElse("null")
      val encodedContent = s"""{"prompt":${prompt.asJson.noSpaces},"options":$optionsJson}"""
      messageRepo.create(chatId, SenderType.System, encodedContent, ContentType.Text, sn.map(_.snakeName)) *>
        outbound.publish(SseEvent.InputRequest(prompt, options, sn)) *>
        inbound.take
    }

  def requestSelection(title: String, items: List[SelectionItem], allowRetry: Boolean): F[String] =
    nodeNameRef.get.flatMap { sn =>
      val event          = SseEvent.SelectionRequest(title, items, allowRetry, sn)
      val encodedContent = event.asJson.noSpaces
      messageRepo.create(chatId, SenderType.System, encodedContent, ContentType.Text, sn.map(_.snakeName)) *>
        outbound.publish(event) *>
        inbound.take
    }

  def setCurrentNode(step: WorkflowStep): F[Unit] = nodeNameRef.set(Some(step))

object LiveChatContext:
  def make[F[_]: Async](
      messageRepo: MessageRepository[F],
      chatId: ChatId
  ): F[(LiveChatContext[F], EventLog[F], Queue[F, String])] =
    for
      eventLog    <- EventLog.make[F]
      inbound     <- Queue.unbounded[F, String]
      nodeNameRef <- Ref.of[F, Option[WorkflowStep]](None)
      ctx = new LiveChatContext[F](eventLog, inbound, nodeNameRef, messageRepo, chatId)
    yield (ctx, eventLog, inbound)
