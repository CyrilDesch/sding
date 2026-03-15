package sding.workflow.task

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import io.circe.Decoder
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import sding.agent.*
import sding.workflow.io.ChatContext
import sding.workflow.io.MessageFormat
import sding.workflow.result.*
import sding.workflow.state.ProjectContextState

class NewTasksSpec extends AsyncWordSpec with AsyncIOSpec with Matchers:

  private def stubAgent(response: String): Agent[IO] = new Agent[IO]:
    val name                                                 = "test-agent"
    def call[A: Decoder](prompt: String): IO[AgentResult[A]] =
      IO {
        io.circe.parser.decode[A](response) match
          case Right(v) => AgentResult.Success(v, name)
          case Left(e)  => AgentResult.Failure(e.getMessage, name)
      }
    def tooledCall[A: Decoder](prompt: String, tools: List[AgentTool[IO]], maxToolCalls: Int): IO[AgentResult[A]] =
      call(prompt)

  private def stubPromptLoader: PromptLoader[IO] = new PromptLoader[IO]:
    def loadSystemPrompt(name: String): IO[String]       = IO.pure("system prompt")
    def loadTaskPrompt(name: String): IO[PromptTemplate] =
      IO.pure(PromptTemplate(name, "Task: {{ project_requirements }}"))

  private val chatCtx: ChatContext[IO] = new ChatContext[IO]:
    def sendMessage(message: String, format: MessageFormat): IO[Unit]           = IO.unit
    def sendState(message: String): IO[Unit]                                    = IO.unit
    def requestInput(prompt: String, options: Option[List[String]]): IO[String] = IO.pure("test-input")

  private val stubSearchTool: AgentTool[IO] = WebSearchTool.stub[IO]

  private val baseState: ProjectContextState = ProjectContextState(
    projectRequirements = Some(ProjectRequirements("B2B", "SaaS")),
    projectLanguage = Some("English"),
    reformulatedProblems = Some(
      ReformulatedProblems(
        List(
          ReformulatedProblem(1, "Problem 1", "devs", "evidence", "situation", "10%", "track bugs")
        )
      )
    )
  )

  "TrendAnalysisTask" should {
    "produce ProblemsTrends from agent responses" in {
      val json =
        """{"problemId":1,"pages":[{"title":"Test","hasComplaint":true,"complaintIntensity":7,"keySnippet":"pain point"}],"complaintCoverage":80,"evidenceStrength":"strong"}"""
      val task = TrendAnalysisTask[IO](stubAgent(json), stubPromptLoader, chatCtx, stubSearchTool)
      task.execute(baseState).asserting { result =>
        result.problemTrends shouldBe defined
        result.problemTrends.get.trends should have length 1
        result.problemTrends.get.trends.head.problemId shouldBe 1
        result.iterationCount.get("trend_analysis") shouldBe Some(1)
      }
    }
  }

  "ProblemSelectionTask" should {
    "compute evidence scores from trends" in {
      val stateWithTrends = baseState.copy(
        problemTrends = Some(
          ProblemsTrends(
            List(
              ProblemTrend(
                1,
                List(Page("Test", true, 8, "snippet")),
                80,
                "strong"
              )
            )
          )
        )
      )
      val task = ProblemSelectionTask[IO](chatCtx)
      task.execute(stateWithTrends).asserting { result =>
        result.problemsSelected shouldBe defined
        result.problemsSelected.get.decisionMatrix should have length 1
        result.problemsSelected.get.decisionMatrix.head.evidenceScore.overallScore should be > 0.0
        result.iterationCount.get("problem_selection") shouldBe Some(1)
      }
    }

    "handle empty trends gracefully" in {
      val task = ProblemSelectionTask[IO](chatCtx)
      task.execute(baseState).asserting { result =>
        result.problemsSelected shouldBe defined
        result.problemsSelected.get.decisionMatrix shouldBe empty
      }
    }
  }

  "UserInterviewsTask" should {
    "run two interviews in parallel" in {
      val json =
        """{"keyQuotes":["quote1"],"painSeverity":4,"currentSolutions":["manual"],"willingnessToPay":3}"""
      val task = UserInterviewsTask[IO](stubAgent(json), stubPromptLoader, chatCtx)
      task.execute(baseState).asserting { result =>
        result.userInterviews shouldBe defined
        result.userInterviews.get.interviews should have length 2
        result.iterationCount.get("user_interviews") shouldBe Some(1)
      }
    }
  }

  "CompetitiveAnalysisTask" should {
    "analyze each SCAMPER variant" in {
      val json =
        """{"scamperId":1,"competitorOfferings":["competitor"],"competitiveAdvantages":["speed"],"distributionAdvantage":["direct"],"opportunity":["gap"],"advantageScore":8}"""
      val stateWithScamper = baseState.copy(
        scamperResult = Some(
          ScamperResult(
            List(
              ScamperVariant(1, 1, "sub", "combine", "adapt", "modify", "put", "elim", "reverse", 7)
            )
          )
        )
      )
      val task = CompetitiveAnalysisTask[IO](stubAgent(json), stubPromptLoader, chatCtx, stubSearchTool)
      task.execute(stateWithScamper).asserting { result =>
        result.competitiveAnalysisResult shouldBe defined
        result.competitiveAnalysisResult.get.vpCompetitiveAnalysis should have length 1
        result.competitiveAnalysisResult.get.vpCompetitiveAnalysis.head.advantageScore shouldBe 8
      }
    }
  }

  "PrototypeBuildsTask" should {
    "build prototypes for each variant" in {
      val storyboardJson =
        """{"scamperId":1,"steps":[{"stepNumber":1,"description":"step1","justification":"reason"}]}"""
      val userTestJson =
        """{"variantGlobalId":1,"initialReaction":"good","perceivedValue":"high","keyQuestions":["q1"],"interestLevel":"high","wouldRecommendScore":8,"wouldPayScore":7,"verbatimQuote":"nice","feedback":{"usabilityHurdle":"none","expectedOutcome":"works","suggestedUiChange":"none","forWhatCanPay":"10","forWhatCanRecommend":"friends"}}"""
      val cardJson =
        """{"scamperId":1,"fiveWs":{"who":"devs","what":"tool","why":"save time","where":"web","when":"daily"},"title":"TestApp","promise":"Save time","personaChips":[{"text":"dev","explanation":"builds"}],"benefitBullets":["fast"],"mvpFeatures":[{"name":"f1","description":"d1","priority":"high"}],"metrics":[{"value":"10x","label":"speed"}],"marketChips":[{"text":"growing","explanation":"trend"}],"socialProofQuote":"love it"}"""

      var callCount  = 0
      val multiAgent = new Agent[IO]:
        val name                                                 = "multi-agent"
        def call[A: Decoder](prompt: String): IO[AgentResult[A]] =
          IO {
            callCount += 1
            val json = callCount % 3 match
              case 1 => storyboardJson
              case 2 => userTestJson
              case 0 => cardJson
            io.circe.parser.decode[A](json) match
              case Right(v) => AgentResult.Success(v, name)
              case Left(e)  => AgentResult.Failure(e.getMessage, name)
          }
        def tooledCall[A: Decoder](prompt: String, tools: List[AgentTool[IO]], maxToolCalls: Int): IO[AgentResult[A]] =
          call(prompt)

      val stateWithScamper = baseState.copy(
        scamperResult = Some(
          ScamperResult(
            List(ScamperVariant(1, 1, "sub", "combine", "adapt", "modify", "put", "elim", "reverse", 7))
          )
        ),
        empathyMapResult = Some(
          EmpathyMapResult(
            EmpathyMap(
              "dev",
              List("code"),
              List("meetings"),
              List("bugs"),
              List("stress"),
              List("bugs"),
              List("quality"),
              List("automate")
            )
          )
        )
      )

      val task = PrototypeBuildsTask[IO](multiAgent, stubPromptLoader, chatCtx)
      task.execute(stateWithScamper).asserting { result =>
        result.prototypeBuildResult shouldBe defined
        result.prototypeBuildResult.get.storyboards should have length 1
        result.userTestResult shouldBe defined
        result.synthetizeProjectResult shouldBe defined
        result.iterationCount.get("prototype_builds") shouldBe Some(1)
      }
    }
  }
