package sding

import cats.effect.IO
import cats.effect.IOApp
import cats.effect.Resource
import cats.syntax.all.*
import com.comcast.ip4s.*
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
import sding.workflow.task.*

object Main extends IOApp.Simple:

  def run: IO[Unit] =
    AppConfig.config.load[IO].flatMap(startServer)

  private def startServer(config: AppConfig): IO[Unit] =
    dataSourceResource(config.postgres).use { ds =>
      val ctx           = new PostgresJdbcContext(SnakeCase, ds)
      val userRepo      = UserRepository.make[IO](ctx)
      val projectRepo   = ProjectRepository.make[IO](ctx)
      val chatRepo      = ChatRepository.make[IO](ctx)
      val messageRepo   = MessageRepository.make[IO](ctx)
      val stepRepo      = StepRepository.make[IO](ctx)
      val encryptionSvc = EncryptionService.make[IO](config.encryption.llmEncryptionKey.value)
      val authSvc       = AuthService.make[IO](userRepo, config.jwt.secret.value, config.jwt.accessTokenExpireSeconds)
      val authMw        = AppAuthMiddleware[IO](authSvc)

      for
        applied <- DatabaseMigrator.migrate[IO](config.postgres)
        _       <- IO.println(s"Flyway: $applied migration(s) applied")

        quotaManager <- LiveQuotaManager.make[IO](10)
        promptLoader <- LivePromptLoader.make[IO]
        tasks = buildTasks(config, quotaManager, promptLoader)
        chatService <- LiveChatService.make[IO](tasks)

        apiRoutes = CorsMiddleware[IO](config.cors.allowedOrigins)(
          AuthRoutes.make[IO](authSvc) <+>
            UserRoutes.make[IO](userRepo, encryptionSvc, authMw) <+>
            ChatRoutes.make[IO](chatService) <+>
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

  private def buildTasks(
      config: AppConfig,
      quotaManager: QuotaManager[IO],
      promptLoader: PromptLoader[IO]
  ): Map[String, TaskNode[IO]] =
    import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel

    val model = GoogleAiGeminiChatModel
      .builder()
      .apiKey(config.llm.apiKey.value)
      .modelName(config.llm.model)
      .build()

    val llmClient   = LiveLlmClient.make[IO](model)
    val agentConfig = AgentConfig("default", "default_system", config.llm.model, 1.0, None)

    val dummyChatCtx = new sding.workflow.io.ChatContext[IO]:
      def sendMessage(m: String, f: sding.workflow.io.MessageFormat): IO[Unit] = IO.unit
      def sendState(m: String): IO[Unit]                                       = IO.unit
      def requestInput(p: String, o: Option[List[String]]): IO[String]         = IO.pure("")

    val systemPrompt = "You are an AI assistant for product brainstorming."
    val agent        = LiveAgent.make[IO](agentConfig, llmClient, systemPrompt, quotaManager)
    val searchTool   = WebSearchTool.stub[IO]

    buildTaskMap(agent, promptLoader, dummyChatCtx, searchTool)

  private def buildTaskMap(
      agent: Agent[IO],
      promptLoader: PromptLoader[IO],
      chatCtx: sding.workflow.io.ChatContext[IO],
      searchTool: AgentTool[IO]
  ): Map[String, TaskNode[IO]] =
    Map(
      "human_requirements"            -> HumanRequirementsTask[IO](chatCtx),
      "weird_problem_generation"      -> WeirdProblemGenerationTask[IO](agent, promptLoader, chatCtx),
      "problem_reformulation"         -> ProblemReformulationTask[IO](agent, promptLoader, chatCtx),
      "trend_analysis"                -> TrendAnalysisTask[IO](agent, promptLoader, chatCtx, searchTool),
      "problem_selection"             -> ProblemSelectionTask[IO](chatCtx),
      "user_interviews"               -> UserInterviewsTask[IO](agent, promptLoader, chatCtx),
      "empathy_map"                   -> EmpathyMapTask[IO](agent, promptLoader, chatCtx),
      "jtbd_definition"               -> JTBDDefinitionTask[IO](agent, promptLoader, chatCtx),
      "hmw"                           -> HMWTask[IO](agent, promptLoader, chatCtx),
      "crazy8s"                       -> Crazy8sTask[IO](agent, promptLoader, chatCtx),
      "scamper"                       -> ScamperTask[IO](agent, promptLoader, chatCtx),
      "competitive_analysis"          -> CompetitiveAnalysisTask[IO](agent, promptLoader, chatCtx, searchTool),
      "prototype_builds"              -> PrototypeBuildsTask[IO](agent, promptLoader, chatCtx),
      "premium_report"                -> PremiumReportTask[IO](agent, promptLoader, chatCtx),
      "human_problem_selection"       -> HumanProblemSelectionTask[IO](chatCtx),
      "human_jtbd_selection"          -> HumanJTBDSelectionTask[IO](chatCtx),
      "human_comprehensive_selection" -> HumanComprehensiveSelectionTask[IO](chatCtx),
      "human_project_selection"       -> HumanProjectSelectionTask[IO](chatCtx),
      "markdown_generation"           -> MarkdownGenerationTask[IO]()
    )
