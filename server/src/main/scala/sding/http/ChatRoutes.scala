package sding.http

import cats.effect.Async
import cats.syntax.all.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.`Content-Type`
import sding.domain.ChatId
import sding.protocol.CreateChatResponse
import sding.protocol.SseEvent
import sding.protocol.SubmitInputRequest
import sding.service.ChatService

object ChatRoutes:

  def make[F[_]: Async](chatService: ChatService[F]): HttpRoutes[F] =
    val dsl = Http4sDsl[F]
    import dsl.*

    HttpRoutes.of[F] {

      case POST -> Root / "chat" =>
        chatService.createChat.flatMap { chatId =>
          Created(CreateChatResponse(chatId.asString).asJson)
        }

      case req @ POST -> Root / "chat" / chatIdStr / "input" =>
        req.as[SubmitInputRequest].flatMap { body =>
          chatService
            .submitInput(ChatId.fromString(chatIdStr), body.input)
            .flatMap(_ => Ok(sding.protocol.StatusResponse("accepted").asJson))
            .handleErrorWith { case e: sding.domain.AppError.ChatError.ChatNotFound =>
              NotFound(sding.protocol.ErrorResponse(e.message).asJson)
            }
        }

      case GET -> Root / "chat" / chatIdStr / "stream" =>
        val chatId    = ChatId.fromString(chatIdStr)
        val sseStream = chatService
          .eventStream(chatId)
          .map { event =>
            val eventType = SseEvent.eventType(event)
            val data      = event.asJson.noSpaces
            ServerSentEvent(Some(data), Some(eventType))
          }
        Ok(sseStream).map(
          _.withContentType(`Content-Type`(MediaType.`text/event-stream`))
        )
    }
