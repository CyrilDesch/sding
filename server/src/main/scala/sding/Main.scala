package sding

import cats.effect.IO
import cats.effect.IOApp
import cats.effect.Resource
import cats.syntax.all.*
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.getquill.*
import org.http4s.HttpRoutes
import org.http4s.StaticFile
import org.http4s.dsl.io.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import sding.agent.*
import sding.auth.AuthService
import sding.config.AppConfig
import sding.config.PostgresSettings
import sding.http.*
import sding.repository.*
import sding.service.*

object Main extends IOApp.Simple:

  def run: IO[Unit] =
    AppConfig.config.load[IO].flatMap(startServer)

  private def startServer(config: AppConfig): IO[Unit] =
    dataSourceResource(config.postgres).use { ds =>
      val ctx           = new PostgresJdbcContext(SnakeCase, ds)
      val userRepo      = UserRepository.make[IO](ctx)
      val encryptionSvc = EncryptionService.make[IO](config.encryption.llmEncryptionKey.value)
      val authSvc       = AuthService.make[IO](userRepo, config.jwt.secret.value, config.jwt.accessTokenExpireSeconds)
      val authMw        = AppAuthMiddleware[IO](authSvc)

      for
        applied <- DatabaseMigrator.migrate[IO](config.postgres)
        _       <- IO.println(s"Flyway: $applied migration(s) applied")

        quotaManager <- LiveQuotaManager.make[IO](10)
        promptLoader <- LivePromptLoader.make[IO]
        chatService  <- LiveChatService.make[IO](userRepo, encryptionSvc, quotaManager, promptLoader)

        apiRoutes = CorsMiddleware[IO](config.cors.allowedOrigins)(
          AuthRoutes.make[IO](authSvc) <+>
            UserRoutes.make[IO](userRepo, encryptionSvc, authMw) <+>
            ChatRoutes.make[IO](chatService, authMw) <+>
            HealthRoutes.make[IO]
        )

        routes = Router(
          config.app.apiPrefix -> apiRoutes,
          "/"                  -> staticRoutes
        ).orNotFound

        _ <- IO.println(s"sding starting on ${config.app.host}:${config.app.port}...")
        _ <- EmberServerBuilder
          .default[IO]
          .withHost(config.app.host)
          .withPort(config.app.port)
          .withHttpApp(routes)
          .build
          .useForever
      yield ()
    }

  private def dataSourceResource(pg: PostgresSettings): Resource[IO, HikariDataSource] =
    Resource.make(
      IO.blocking {
        val hc = new HikariConfig()
        hc.setJdbcUrl(s"jdbc:postgresql://${pg.host}:${pg.port}/${pg.database}?stringtype=unspecified")
        hc.setUsername(pg.user)
        hc.setPassword(pg.password.value)
        hc.setMaximumPoolSize(pg.poolSize)
        new HikariDataSource(hc)
      }
    )(ds => IO.blocking(ds.close()))

  private val staticRoutes: HttpRoutes[IO] =
    HttpRoutes.of[IO] { case req @ GET -> path =>
      val pathStr  = path.renderString
      val resource =
        if pathStr == "/" || pathStr.isEmpty then "/static/index.html"
        else s"/static$pathStr"
      StaticFile
        .fromResource(resource, Some(req))
        .getOrElseF(
          StaticFile.fromResource("/static/index.html", Some(req)).getOrElseF(NotFound())
        )
    }
