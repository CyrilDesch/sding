package sding.http

import cats.data.OptionT
import cats.effect.Async
import cats.syntax.all.*
import io.circe.syntax.*
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import sding.protocol.ErrorResponse

object ErrorHandlingMiddleware:

  def apply[F[_]: Async](routes: HttpRoutes[F]): HttpRoutes[F] =
    val dsl = Http4sDsl[F]
    import dsl.*
    HttpRoutes[F] { req =>
      routes(req).handleErrorWith { e =>
        OptionT.liftF(
          Async[F].delay(
            scribe.error(s"Unhandled error on ${req.method} ${req.uri}: ${e.getClass.getSimpleName}: ${e.getMessage}", e)
          ) *>
            InternalServerError(ErrorResponse("Internal server error").asJson)
        )
      }
    }
