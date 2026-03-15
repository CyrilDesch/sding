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
import org.typelevel.otel4s.sdk.OpenTelemetrySdk
import org.typelevel.otel4s.sdk.exporter.otlp.autoconfigure.OtlpExportersAutoConfigure
import org.typelevel.otel4s.trace.Tracer
import sding.agent.LivePromptLoader
import sding.auth.AuthService
import sding.config.AppConfig
import sding.config.LangfuseSettings
import sding.config.PostgresSettings
import sding.http.*
import sding.repository.*
import sding.service.*

object Main extends IOApp.Simple:

  def run: IO[Unit] =
    IO(configureLogging()) >> AppConfig.config.load[IO].flatMap(startWithOtel)

  private def configureLogging(): Unit =
    scribe.Logger("dev.langchain4j.internal.RetryUtils").orphan().replace()

  private def startWithOtel(config: AppConfig): IO[Unit] =
    configureOtelProps(config.langfuse) >>
      OpenTelemetrySdk
        .autoConfigured[IO](_.addExportersConfigurer(OtlpExportersAutoConfigure[IO]))
        .use { sdk =>
          sdk.sdk.tracerProvider.get("sding").flatMap { tracer =>
            given Tracer[IO] = tracer
            startServer(config)
          }
        }

  private def configureOtelProps(lf: LangfuseSettings): IO[Unit] =
    IO.delay {
      if lf.enabled then
        val auth = java.util.Base64.getEncoder.encodeToString(
          s"${lf.publicKey.value}:${lf.secretKey.value}".getBytes
        )
        val headers = s"Authorization=Basic $auth,x-langfuse-ingestion-version=4"
        System.setProperty("otel.service.name", "sding")
        System.setProperty("otel.traces.exporter", "otlp")
        System.setProperty("otel.metrics.exporter", "none")
        System.setProperty("otel.logs.exporter", "none")
        System.setProperty("otel.exporter.otlp.endpoint", s"${lf.baseUrl}/api/public/otel")
        System.setProperty("otel.exporter.otlp.headers", headers)
        System.setProperty("otel.exporter.otlp.protocol", "http/protobuf")
      else
        System.setProperty("otel.traces.exporter", "none")
        System.setProperty("otel.metrics.exporter", "none")
        System.setProperty("otel.logs.exporter", "none")
    }

  private def startServer(config: AppConfig)(using Tracer[IO]): IO[Unit] =
    dataSourceResource(config.postgres).use { ds =>
      val ctx           = new PostgresJdbcContext(SnakeCase, ds)
      val userRepo      = UserRepository.make[IO](ctx)
      val projectRepo   = sding.repository.ProjectRepository.make[IO](ctx)
      val chatRepo      = sding.repository.ChatRepository.make[IO](ctx)
      val stepRepo      = sding.repository.StepRepository.make[IO](ctx)
      val messageRepo   = sding.repository.MessageRepository.make[IO](ctx)
      val encryptionSvc = EncryptionService.make[IO](config.encryption.llmEncryptionKey.value)
      val authSvc       = AuthService.make[IO](userRepo, config.jwt.secret.value, config.jwt.accessTokenExpireSeconds)
      val authMw        = AppAuthMiddleware[IO](authSvc)

      for
        applied <- DatabaseMigrator.migrate[IO](config.postgres)
        _       <- IO.println(s"Flyway: $applied migration(s) applied")

        promptLoader <- LivePromptLoader.make[IO]
        chatService  <- LiveChatService
          .make[IO](userRepo, projectRepo, chatRepo, stepRepo, messageRepo, encryptionSvc, promptLoader)

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
