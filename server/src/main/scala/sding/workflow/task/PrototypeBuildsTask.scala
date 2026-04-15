package sding.workflow.task

import cats.effect.Async
import cats.effect.implicits.*
import cats.syntax.all.*
import chat4s.ai.Agent
import chat4s.ai.AgentResult
import chat4s.ai.JsonSchemaOf
import chat4s.ai.prompt.PromptLink
import chat4s.ai.prompt.PromptLoader
import chat4s.ai.prompt.PromptTemplate
import chat4s.io.ChatContext
import io.circe.Decoder
import io.circe.Encoder
import sding.protocol.WorkflowStep
import sding.workflow.TaskNode
import sding.workflow.state.ProjectContextState

final case class StoryboardStep(stepNumber: Int, description: String, justification: String)
    derives Decoder,
      Encoder.AsObject,
      JsonSchemaOf
final case class Storyboard(scamperId: Int, steps: List[StoryboardStep]) derives Decoder, Encoder.AsObject, JsonSchemaOf
final case class PrototypeBuildResult(storyboards: List[Storyboard]) derives Decoder, Encoder.AsObject, JsonSchemaOf

final case class StructuredFeedback(
    usabilityHurdle: String,
    expectedOutcome: String,
    suggestedUiChange: String,
    forWhatCanPay: String,
    forWhatCanRecommend: String
) derives Decoder,
      Encoder.AsObject,
      JsonSchemaOf
final case class UserTest(
    variantGlobalId: Int,
    initialReaction: String,
    perceivedValue: String,
    keyQuestions: List[String],
    interestLevel: String,
    wouldRecommendScore: Int,
    wouldPayScore: Int,
    verbatimQuote: String,
    feedback: StructuredFeedback
) derives Decoder,
      Encoder.AsObject,
      JsonSchemaOf
final case class UserTestsResult(userTests: List[UserTest]) derives Decoder, Encoder.AsObject, JsonSchemaOf

final case class FiveWs(who: String, what: String, why: String, where: String, when: String)
    derives Decoder,
      Encoder.AsObject,
      JsonSchemaOf
final case class Chip(text: String, explanation: String) derives Decoder, Encoder.AsObject, JsonSchemaOf
final case class Metric(value: String, label: String) derives Decoder, Encoder.AsObject, JsonSchemaOf
final case class MVPFeature(name: String, description: String, priority: String)
    derives Decoder,
      Encoder.AsObject,
      JsonSchemaOf
final case class ProjectCardSchema(
    scamperId: Int,
    fiveWs: FiveWs,
    title: String,
    promise: String,
    personaChips: List[Chip],
    benefitBullets: List[String],
    mvpFeatures: List[MVPFeature],
    metrics: List[Metric],
    marketChips: List[Chip],
    socialProofQuote: String
) derives Decoder,
      Encoder.AsObject,
      JsonSchemaOf
final case class SynthetizeProjectsResult(projectsCards: List[ProjectCardSchema])
    derives Decoder,
      Encoder.AsObject,
      JsonSchemaOf

final class PrototypeBuildsTask[F[_]: Async](
    agent: Agent[F],
    promptLoader: PromptLoader[F],
    chatContext: ChatContext[F]
) extends TaskNode[F]:
  val name = WorkflowStep.PrototypeBuilds

  private val maxRetries = 3
  private val minScore   = 5

  def execute(state: ProjectContextState): F[ProjectContextState] =
    val variants = state.scamperResult.map(_.scamperVariants).getOrElse(Nil)
    for
      _            <- chatContext.sendState(s"Building prototypes for ${variants.length} variants...")
      ptBuild      <- promptLoader.loadTaskPrompt("PrototypeBuildTask")
      ptTest       <- promptLoader.loadTaskPrompt("UserTestTask")
      ptSynthetize <- promptLoader.loadTaskPrompt("SynthetizeProjectTask")
      baseVars = Map(
        "project_requirements" -> state.projectRequirements.map(_.toString).getOrElse(""),
        "empathy_map_result"   -> state.empathyMapResult.map(_.toString).getOrElse(""),
        "project_language"     -> state.projectLanguage.getOrElse("English")
      )
      sessionId = chatContext.sessionId
      results <- variants.parTraverse { variant =>
        processVariant(variant, baseVars, ptBuild, ptTest, ptSynthetize, sessionId)
      }
      storyboards    = results.map(_._1)
      userTests      = results.map(_._2)
      cards          = results.map(_._3)
      (nextState, _) = state.incrementIteration(name)
    yield nextState.copy(
      prototypeBuildResult = Some(PrototypeBuildResult(storyboards)),
      userTestResult = Some(UserTestsResult(userTests)),
      synthetizeProjectResult = Some(SynthetizeProjectsResult(cards))
    )

  private def processVariant(
      variant: ScamperVariant,
      baseVars: Map[String, String],
      ptBuild: PromptTemplate,
      ptTest: PromptTemplate,
      ptSynthetize: PromptTemplate,
      sessionId: String
  ): F[(Storyboard, UserTest, ProjectCardSchema)] =
    val variantVars    = baseVars ++ Map("scamper_variant" -> variant.toString)
    val buildLink      = PromptLink(ptBuild.name, ptBuild.version, sessionId)
    val testLink       = PromptLink(ptTest.name, ptTest.version, sessionId)
    val synthetizeLink = PromptLink(ptSynthetize.name, ptSynthetize.version, sessionId)

    def buildAndTest(iteration: Int): F[(Storyboard, UserTest)] =
      for
        buildResult <- agent.call[Storyboard](ptBuild.render(variantVars), buildLink)
        storyboard  <- buildResult match
          case AgentResult.Success(v, _) => Async[F].pure(v)
          case AgentResult.Failure(m, _) =>
            Async[F].raiseError(sding.domain.AppError.AgentError.LlmInvocationFailed(name, m))
        testPromptVars = variantVars ++ Map("storyboard" -> storyboard.toString)
        testResult <- agent.call[UserTest](ptTest.render(testPromptVars), testLink)
        userTest   <- testResult match
          case AgentResult.Success(v, _) => Async[F].pure(v)
          case AgentResult.Failure(m, _) =>
            Async[F].raiseError(sding.domain.AppError.AgentError.LlmInvocationFailed(name, m))
        result <-
          if (userTest.wouldPayScore < minScore || userTest.wouldRecommendScore < minScore) && iteration < maxRetries
          then buildAndTest(iteration + 1)
          else Async[F].pure((storyboard, userTest))
      yield result

    for
      (storyboard, userTest) <- buildAndTest(1)
      synthVars = variantVars ++ Map(
        "storyboard" -> storyboard.toString,
        "user_test"  -> userTest.toString
      )
      synthResult <- agent.call[ProjectCardSchema](ptSynthetize.render(synthVars), synthetizeLink)
      card        <- synthResult match
        case AgentResult.Success(v, _) => Async[F].pure(v)
        case AgentResult.Failure(m, _) =>
          Async[F].raiseError(sding.domain.AppError.AgentError.LlmInvocationFailed(name, m))
    yield (storyboard, userTest, card)
