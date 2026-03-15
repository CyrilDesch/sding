package sding.http

import cats.effect.Async
import org.http4s.HttpRoutes
import org.http4s.Method
import org.http4s.headers.Origin
import org.http4s.server.middleware.CORS

object CorsMiddleware:

  def apply[F[_]: Async](allowedOrigins: List[String])(routes: HttpRoutes[F]): HttpRoutes[F] =
    val policy = CORS.policy
      .withAllowMethodsIn(Set(Method.GET, Method.POST, Method.PUT, Method.DELETE, Method.OPTIONS))
      .withAllowCredentials(false)

    val withOrigins =
      if allowedOrigins.contains("*") then policy.withAllowOriginAll
      else
        policy.withAllowOriginHost { (origin: Origin.Host) =>
          allowedOrigins.exists(o => origin.renderString.contains(o))
        }

    withOrigins.apply(routes)
