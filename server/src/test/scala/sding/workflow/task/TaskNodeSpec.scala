package sding.workflow.task

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import io.circe.Decoder
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import sding.agent.*
import sding.workflow.io.ChatContext
import sding.workflow.io.MessageFormat
import sding.workflow.io.UserInputRequest
import sding.workflow.result.*
import sding.workflow.state.ProjectContextState

class TaskNodeSpec extends AsyncWordSpec with AsyncIOSpec with Matchers:

  private def stubAgent(response: String): Agent[IO] = new Agent[IO]:
    val name                                                               = "test-agent"
    def call[A: Decoder: JsonSchemaOf](prompt: String): IO[AgentResult[A]] =
      IO {
        io.circe.parser.decode[A](response) match
          case Right(v) => AgentResult.Success(v, name)
          case Left(e)  => AgentResult.Failure(e.getMessage, name)
      }
    def tooledCall[A: Decoder: JsonSchemaOf](
        prompt: String,
        tools: List[AgentTool[IO]],
        maxToolCalls: Int
    ): IO[AgentResult[A]] =
      call(prompt)

  private def stubPromptLoader: PromptLoader[IO] = new PromptLoader[IO]:
    def loadSystemPrompt(name: String): IO[String]       = IO.pure("system prompt")
    def loadTaskPrompt(name: String): IO[PromptTemplate] =
      IO.pure(PromptTemplate(name, "Task: {{ project_requirements }}"))

  private def recordingChatContext: ChatContext[IO] = new ChatContext[IO]:
    def sendMessage(message: String, format: MessageFormat): IO[Unit] = IO.unit
    def sendState(message: String): IO[Unit]                          = IO.unit
    def requestInput(request: UserInputRequest): IO[String]           = IO.pure("test-input")
    def requestSelection(title: String, items: List[sding.protocol.SelectionItem], allowRetry: Boolean): IO[String] =
      IO.pure("test-input")

  val baseState: ProjectContextState = ProjectContextState(
    projectRequirements = Some(ProjectRequirements("B2B", "SaaS")),
    projectLanguage = Some("English")
  )

  "WeirdProblemGenerationTask" should {
    "produce WeirdProblems from agent response" in {
      val json = """{"problems":[{"id":1,"statement":"problem","fullStatement":"full problem"}]}"""
      val task = WeirdProblemGenerationTask[IO](stubAgent(json), stubPromptLoader, recordingChatContext)
      task.execute(baseState).asserting { result =>
        result.weirdProblems shouldBe defined
        result.weirdProblems.get.problems should have length 1
        result.weirdProblems.get.problems.head.id shouldBe 1
      }
    }

    "increment iteration count" in {
      val json = """{"problems":[{"id":1,"statement":"s","fullStatement":"fs"}]}"""
      val task = WeirdProblemGenerationTask[IO](stubAgent(json), stubPromptLoader, recordingChatContext)
      task.execute(baseState).asserting { result =>
        result.iterationCount.get("weird_problem_generation") shouldBe Some(1)
      }
    }

    "propagate agent failure as error" in {
      val failAgent = new Agent[IO]:
        val name                                                               = "fail-agent"
        def call[A: Decoder: JsonSchemaOf](prompt: String): IO[AgentResult[A]] =
          IO.pure(AgentResult.Failure("LLM failed", name))
        def tooledCall[A: Decoder: JsonSchemaOf](
            prompt: String,
            tools: List[AgentTool[IO]],
            maxToolCalls: Int
        ): IO[AgentResult[A]] =
          call(prompt)

      val task = WeirdProblemGenerationTask[IO](failAgent, stubPromptLoader, recordingChatContext)
      task.execute(baseState).assertThrows[sding.domain.AppError.AgentError.LlmInvocationFailed]
    }
  }

  "EmpathyMapTask" should {
    "produce EmpathyMapResult from agent response" in {
      val json =
        """{"empathyMap":{"personaDescription":"dev","see":["code"],"hear":["meetings"],"think":["deadlines"],"feel":["stress"],"pains":["bugs"],"desiredOutcomes":["quality"],"insights":["automate"]}}"""
      val task = EmpathyMapTask[IO](stubAgent(json), stubPromptLoader, recordingChatContext)
      task.execute(baseState).asserting { result =>
        result.empathyMapResult shouldBe defined
        result.empathyMapResult.get.empathyMap.personaDescription shouldBe "dev"
      }
    }
  }

  "HumanRequirementsTask" should {
    "collect requirements from chat context" in {
      var prompts = List.empty[String]
      val chatCtx = new ChatContext[IO]:
        def sendMessage(message: String, format: MessageFormat): IO[Unit] = IO.unit
        def sendState(message: String): IO[Unit]                          = IO.unit
        def requestInput(request: UserInputRequest): IO[String]           =
          IO {
            val prompt = request match
              case UserInputRequest.FreeText(p)  => p
              case UserInputRequest.Choice(p, _) => p
            prompts = prompts :+ prompt
            prompt match
              case p if p.contains("Market")   => "B2C"
              case p if p.contains("Software") => "Mobile App"
              case _                           => "Help users track habits"
          }
        def requestSelection(
            title: String,
            items: List[sding.protocol.SelectionItem],
            allowRetry: Boolean
        ): IO[String] =
          IO.pure("test-input")

      val task = HumanRequirementsTask[IO](chatCtx)
      task.execute(ProjectContextState()).asserting { result =>
        result.projectRequirements shouldBe defined
        result.projectRequirements.get.projectType shouldBe "B2C"
        result.projectRequirements.get.projectSoftwareType shouldBe "Mobile App"
        result.projectRequirements.get.projectProblem shouldBe Some("Help users track habits")
      }
    }
  }

  "MarkdownGenerationTask" should {
    "generate markdown from premium report" in {
      val state = baseState.copy(
        premiumReportResult = Some(
          PremiumReportResult(
            premiumReport = PremiumReportSchema(
              executiveSummary = "Summary",
              problemContext = ProblemContext("problem", Nil, Nil, "scope", "urgent"),
              marketContext = MarketContext(Nil, Nil, Nil, "large", "now"),
              strategicRationale = StrategicRationale("aligned", "unique", Nil, "high", Nil),
              validationEvidence = ValidationEvidence(Nil, Nil, Nil, Nil, Nil),
              executionReadiness = ExecutionReadiness(Nil, "low", "agile", "direct", Nil),
              alternativeExploration = AlternativeExploration(Nil, "best fit", Nil, Nil),
              selectedProjectDetails = SelectedProjectDetails("MyApp", "core", Nil, Nil, Nil, "P0"),
              methodologyUsed = Nil,
              dataSources = Nil,
              recommendation = "go",
              confidenceLevel = "high"
            ),
            reportMetadata = Map("version" -> "1")
          )
        )
      )

      val task = MarkdownGenerationTask[IO]()
      task.execute(state).asserting { result =>
        result.markdownGenerationResult shouldBe defined
        result.markdownGenerationResult.get.reportTitle shouldBe "MyApp"
        result.markdownGenerationResult.get.markdownContent should include("MyApp")
        result.isFinalNode shouldBe true
      }
    }
  }
