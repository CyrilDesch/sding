package sding.http

import cats.effect.Async
import io.circe.Json
import io.circe.syntax.*
import org.http4s.HttpRoutes
import org.http4s.circe.*
import org.http4s.dsl.Http4sDsl

object HealthRoutes:

  def make[F[_]: Async]: HttpRoutes[F] =
    val dsl = Http4sDsl[F]
    import dsl.*

    HttpRoutes.of[F] { case GET -> Root / "health" =>
      Ok(
        Json.obj(
          "status"  -> "ok".asJson,
          "service" -> "sding".asJson,
          "version" -> sding.BuildInfo.version.asJson
        )
      )
    }
