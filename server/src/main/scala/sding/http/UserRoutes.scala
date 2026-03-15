package sding.http

import cats.effect.Async
import cats.syntax.all.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware
import sding.auth.AuthUser
import sding.protocol.LlmConfigRequest
import sding.protocol.LlmConfigResponse
import sding.repository.UserRepository
import sding.service.EncryptionService

object UserRoutes:

  def make[F[_]: Async](
      userRepo: UserRepository[F],
      encryptionService: EncryptionService[F],
      authMiddleware: AuthMiddleware[F, AuthUser]
  ): HttpRoutes[F] =
    val dsl = Http4sDsl[F]
    import dsl.*

    val authedRoutes = AuthedRoutes.of[AuthUser, F] {

      case GET -> Root / "user" / "llm-config" as user =>
        userRepo.getLlmConfig(user.id).flatMap {
          case Some((provider, _, model)) =>
            Ok(LlmConfigResponse(provider, model, "***configured***").asJson)
          case None =>
            Ok(sding.protocol.ErrorResponse("No LLM configuration found").asJson)
        }

      case req @ PUT -> Root / "user" / "llm-config" as user =>
        req.req.as[LlmConfigRequest].flatMap { body =>
          for
            encrypted <- encryptionService.encrypt(body.apiKey)
            _         <- userRepo.updateLlmConfig(user.id, body.provider, encrypted, body.model)
            resp      <- Ok(LlmConfigResponse(body.provider, body.model, maskKey(body.apiKey)).asJson)
          yield resp
        }
    }

    authMiddleware(authedRoutes)

  private def maskKey(key: String): String =
    if key.length <= 8 then "***"
    else s"${key.take(4)}...${key.takeRight(4)}"
