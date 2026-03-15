package sding.http

import cats.data.Kleisli
import cats.data.OptionT
import cats.effect.Async
import cats.syntax.all.*
import org.http4s.*
import org.http4s.server
import sding.auth.AuthService
import sding.auth.AuthUser

object AppAuthMiddleware:
  private val cookieName = "sding_token"

  def apply[F[_]: Async](authService: AuthService[F]): server.AuthMiddleware[F, AuthUser] =
    val authUser: Kleisli[[x] =>> OptionT[F, x], Request[F], AuthUser] =
      Kleisli { req =>
        OptionT {
          req.cookies.find(_.name == cookieName) match
            case Some(cookie) =>
              authService
                .verifyToken(cookie.content)
                .map(Some(_))
                .handleErrorWith { e =>
                  Async[F].delay(
                    scribe.warn(s"Auth failed for ${req.method} ${req.uri}: ${e.getMessage}")
                  ) *> Async[F].pure(None)
                }
            case None =>
              Async[F].delay(
                scribe.debug(s"No auth cookie for ${req.method} ${req.uri}")
              ) *> Async[F].pure(None)
        }
      }
    server.AuthMiddleware.withFallThrough(authUser)
