package sding.http

import cats.data.Kleisli
import cats.data.OptionT
import cats.effect.Async
import cats.syntax.all.*
import org.http4s.*
import org.http4s.headers.Authorization
import org.http4s.server
import sding.auth.AuthService
import sding.auth.AuthUser

object AppAuthMiddleware:
  def apply[F[_]: Async](authService: AuthService[F]): server.AuthMiddleware[F, AuthUser] =
    val authUser: Kleisli[[x] =>> OptionT[F, x], Request[F], AuthUser] =
      Kleisli { req =>
        OptionT {
          req.headers.get[Authorization] match
            case Some(Authorization(Credentials.Token(AuthScheme.Bearer, token))) =>
              authService.verifyToken(token).map(Some(_)).handleError(_ => None)
            case _ => Async[F].pure(None)
        }
      }
    server.AuthMiddleware(authUser)
