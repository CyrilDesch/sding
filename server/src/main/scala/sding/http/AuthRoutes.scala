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
import sding.protocol.ErrorResponse
import sding.protocol.LoginRequest
import sding.protocol.RegisterRequest
import sding.protocol.StatusResponse

object AuthRoutes:
  private val cookieName = "sding_token"

  private def tokenCookie(token: String): ResponseCookie =
    ResponseCookie(
      cookieName,
      token,
      httpOnly = true,
      sameSite = Some(SameSite.Strict),
      path = Some("/")
    )

  private val clearCookie: ResponseCookie =
    ResponseCookie(
      cookieName,
      "",
      httpOnly = true,
      sameSite = Some(SameSite.Strict),
      path = Some("/"),
      maxAge = Some(0)
    )

  def make[F[_]: Async](authService: AuthService[F]): HttpRoutes[F] =
    val dsl = Http4sDsl[F]
    import dsl.*
    HttpRoutes.of[F] {
      case req @ POST -> Root / "auth" / "register" =>
        req.as[RegisterRequest].flatMap { body =>
          authService
            .register(body.email, body.password, body.firstName, body.lastName)
            .flatMap(token => Created(StatusResponse("ok").asJson).map(_.addCookie(tokenCookie(token.token))))
            .handleErrorWith { case e: AppError.AuthError =>
              Conflict(ErrorResponse(e.message).asJson)
            }
        }

      case req @ POST -> Root / "auth" / "login" =>
        req.as[LoginRequest].flatMap { body =>
          authService
            .login(body.email, body.password)
            .flatMap(token => Ok(StatusResponse("ok").asJson).map(_.addCookie(tokenCookie(token.token))))
            .handleErrorWith { case e: AppError.AuthError =>
              Unauthorized(
                `WWW-Authenticate`(Challenge("Bearer", "sding")),
                ErrorResponse(e.message).asJson
              )
            }
        }

      case req @ GET -> Root / "auth" / "me" =>
        req.cookies.find(_.name == cookieName) match
          case None =>
            Async[F].delay(scribe.debug("GET /auth/me: no auth cookie")) *>
              Async[F].pure(Response[F](Status.Unauthorized))
          case Some(cookie) =>
            authService
              .verifyToken(cookie.content)
              .flatMap(_ => Ok(StatusResponse("ok").asJson))
              .handleErrorWith { e =>
                Async[F].delay(scribe.warn(s"GET /auth/me: verification failed: ${e.getMessage}")) *>
                  Async[F].pure(Response[F](Status.Unauthorized))
              }

      case POST -> Root / "auth" / "logout" =>
        Ok(StatusResponse("ok").asJson).map(_.addCookie(clearCookie))
    }
