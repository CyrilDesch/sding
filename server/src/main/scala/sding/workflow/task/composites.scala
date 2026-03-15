package sding.workflow.task

import cats.effect.Async
import cats.effect.implicits.*
import cats.syntax.all.*
import sding.agent.Agent
import sding.agent.AgentResult
import sding.agent.PromptLoader
import sding.protocol.WorkflowStep
import sding.workflow.io.ChatContext
import sding.workflow.result.*
import sding.workflow.state.ProjectContextState

final class UserInterviewsTask[F[_]: Async](
    agent: Agent[F],
    promptLoader: PromptLoader[F],
    chatContext: ChatContext[F]
) extends TaskNode[F]:
  val name = WorkflowStep.UserInterviews

  def execute(state: ProjectContextState): F[ProjectContextState] =
    for
      _  <- chatContext.sendState("Running parallel user interviews...")
      pt <- promptLoader.loadTaskPrompt("UserInterviewTask")
      baseVars = Map(
        "project_requirements"  -> state.projectRequirements.map(_.toString).getOrElse(""),
        "reformulated_problems" -> state.reformulatedProblems.map(_.toString).getOrElse(""),
        "project_language"      -> state.projectLanguage.getOrElse("English")
      )
      hyperPrompt   = pt.render(baseVars ++ Map("persona" -> "hyper_concerned_user"))
      skepticPrompt = pt.render(baseVars ++ Map("persona" -> "skeptical_user"))
      (hyperResult, skepticResult) <- (
        agent.call[Interview](hyperPrompt),
        agent.call[Interview](skepticPrompt)
      ).parTupled
      interviews <- (hyperResult, skepticResult) match
        case (AgentResult.Success(h, _), AgentResult.Success(s, _)) =>
          Async[F].pure(UserInterviewResult(List(h, s)))
        case (AgentResult.Failure(msg, _), _) =>
          Async[F].raiseError(sding.domain.AppError.AgentError.LlmInvocationFailed(name, msg))
        case (_, AgentResult.Failure(msg, _)) =>
          Async[F].raiseError(sding.domain.AppError.AgentError.LlmInvocationFailed(name, msg))
      (nextState, _) = state.incrementIteration(name)
    yield nextState.copy(userInterviews = Some(interviews))

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
      results <- variants.parTraverse { variant =>
        processVariant(variant, baseVars, ptBuild, ptTest, ptSynthetize)
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
      ptBuild: sding.agent.PromptTemplate,
      ptTest: sding.agent.PromptTemplate,
      ptSynthetize: sding.agent.PromptTemplate
  ): F[(Storyboard, UserTest, ProjectCardSchema)] =
    val variantVars = baseVars ++ Map("scamper_variant" -> variant.toString)

    def buildAndTest(iteration: Int): F[(Storyboard, UserTest)] =
      for
        buildResult <- agent.call[Storyboard](ptBuild.render(variantVars))
        storyboard  <- buildResult match
          case AgentResult.Success(v, _) => Async[F].pure(v)
          case AgentResult.Failure(m, _) =>
            Async[F].raiseError(sding.domain.AppError.AgentError.LlmInvocationFailed(name, m))
        testPromptVars = variantVars ++ Map("storyboard" -> storyboard.toString)
        testResult <- agent.call[UserTest](ptTest.render(testPromptVars))
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
      synthResult <- agent.call[ProjectCardSchema](ptSynthetize.render(synthVars))
      card        <- synthResult match
        case AgentResult.Success(v, _) => Async[F].pure(v)
        case AgentResult.Failure(m, _) =>
          Async[F].raiseError(sding.domain.AppError.AgentError.LlmInvocationFailed(name, m))
    yield (storyboard, userTest, card)
