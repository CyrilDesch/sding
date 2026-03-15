package sding.http

import cats.effect.Async
import cats.syntax.all.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.`WWW-Authenticate`
import sding.auth.AuthService
import sding.domain.AppError
import sding.protocol.AuthTokenResponse
import sding.protocol.LoginRequest
import sding.protocol.RegisterRequest

object AuthRoutes:
  def make[F[_]: Async](authService: AuthService[F]): HttpRoutes[F] =
    val dsl = Http4sDsl[F]
    import dsl.*
    HttpRoutes.of[F] {
      case req @ POST -> Root / "auth" / "register" =>
        req.as[RegisterRequest].flatMap { body =>
          authService
            .register(body.email, body.password, body.firstName, body.lastName)
            .flatMap(token => Created(AuthTokenResponse(token.token, token.expiresAt.getEpochSecond).asJson))
            .handleErrorWith { case e: AppError.AuthError =>
              Conflict(sding.protocol.ErrorResponse(e.message).asJson)
            }
        }
      case req @ POST -> Root / "auth" / "login" =>
        req.as[LoginRequest].flatMap { body =>
          authService
            .login(body.email, body.password)
            .flatMap(token => Ok(AuthTokenResponse(token.token, token.expiresAt.getEpochSecond).asJson))
            .handleErrorWith { case e: AppError.AuthError =>
              Unauthorized(
                `WWW-Authenticate`(Challenge("Bearer", "sding")),
                sding.protocol.ErrorResponse(e.message).asJson
              )
            }
        }
    }
