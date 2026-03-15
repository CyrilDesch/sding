package sding.workflow.task

import cats.effect.Async
import cats.syntax.all.*
import sding.agent.Agent
import sding.agent.AgentResult
import sding.agent.AgentTool
import sding.agent.PromptLoader
import sding.protocol.WorkflowStep
import sding.workflow.io.ChatContext
import sding.workflow.io.UserInputRequest.*
import sding.workflow.result.*
import sding.workflow.state.ProjectContextState

final class WeirdProblemGenerationTask[F[_]: Async](
    agent: Agent[F],
    promptLoader: PromptLoader[F],
    chatContext: ChatContext[F]
) extends AgentTaskNode[F](agent, promptLoader, chatContext):
  val name       = WorkflowStep.WeirdProblemGeneration
  val promptName = "WeirdProblemGenerationTask"

  def templateVars(state: ProjectContextState): Map[String, String] =
    Map(
      "project_requirements" -> state.projectRequirements.map(_.toString).getOrElse(""),
      "project_language"     -> state.projectLanguage.getOrElse("English"),
      "noun_list"            -> "cloud, bicycle, mirror, garden, lighthouse"
    )

  def updateState[A](state: ProjectContextState, result: A): ProjectContextState =
    result match
      case wp: WeirdProblems => state.copy(weirdProblems = Some(wp))
      case _                 => state

  def execute(state: ProjectContextState): F[ProjectContextState] =
    runAgent[WeirdProblems](state)

final class ProblemReformulationTask[F[_]: Async](
    agent: Agent[F],
    promptLoader: PromptLoader[F],
    chatContext: ChatContext[F]
) extends AgentTaskNode[F](agent, promptLoader, chatContext):
  val name       = WorkflowStep.ProblemReformulation
  val promptName = "ProblemReformulationTask"

  def templateVars(state: ProjectContextState): Map[String, String] =
    Map(
      "project_requirements" -> state.projectRequirements.map(_.toString).getOrElse(""),
      "original_problem"     -> state.weirdProblems.map(_.toString).getOrElse(""),
      "empathy_map_result"   -> state.empathyMapResult.map(_.toString).getOrElse(""),
      "project_language"     -> state.projectLanguage.getOrElse("English")
    )

  def updateState[A](state: ProjectContextState, result: A): ProjectContextState =
    result match
      case rp: ReformulatedProblems => state.copy(reformulatedProblems = Some(rp))
      case _                        => state

  def execute(state: ProjectContextState): F[ProjectContextState] =
    runAgent[ReformulatedProblems](state)

final class EmpathyMapTask[F[_]: Async](
    agent: Agent[F],
    promptLoader: PromptLoader[F],
    chatContext: ChatContext[F]
) extends AgentTaskNode[F](agent, promptLoader, chatContext):
  val name       = WorkflowStep.EmpathyMap
  val promptName = "EmpathyMapTask"

  def templateVars(state: ProjectContextState): Map[String, String] =
    Map(
      "project_requirements" -> state.projectRequirements.map(_.toString).getOrElse(""),
      "project_language"     -> state.projectLanguage.getOrElse("English")
    )

  def updateState[A](state: ProjectContextState, result: A): ProjectContextState =
    result match
      case em: EmpathyMapResult => state.copy(empathyMapResult = Some(em))
      case _                    => state

  def execute(state: ProjectContextState): F[ProjectContextState] =
    runAgent[EmpathyMapResult](state)

final class JTBDDefinitionTask[F[_]: Async](
    agent: Agent[F],
    promptLoader: PromptLoader[F],
    chatContext: ChatContext[F]
) extends AgentTaskNode[F](agent, promptLoader, chatContext):
  val name       = WorkflowStep.JtbdDefinition
  val promptName = "JTBDDefinitionTask"

  def templateVars(state: ProjectContextState): Map[String, String] =
    Map(
      "project_requirements" -> state.projectRequirements.map(_.toString).getOrElse(""),
      "empathy_map_result"   -> state.empathyMapResult.map(_.toString).getOrElse(""),
      "project_language"     -> state.projectLanguage.getOrElse("English")
    )

  def updateState[A](state: ProjectContextState, result: A): ProjectContextState =
    result match
      case jd: JTBDDefinitionResult => state.copy(jtbdDefinitionResult = Some(jd))
      case _                        => state

  def execute(state: ProjectContextState): F[ProjectContextState] =
    runAgent[JTBDDefinitionResult](state)

final class HMWTask[F[_]: Async](
    agent: Agent[F],
    promptLoader: PromptLoader[F],
    chatContext: ChatContext[F]
) extends AgentTaskNode[F](agent, promptLoader, chatContext):
  val name       = WorkflowStep.Hmw
  val promptName = "HMWTask"

  def templateVars(state: ProjectContextState): Map[String, String] =
    Map(
      "project_requirements" -> state.projectRequirements.map(_.toString).getOrElse(""),
      "empathy_map_result"   -> state.empathyMapResult.map(_.toString).getOrElse(""),
      "jtbd_result"          -> state.jtbdDefinitionResult.map(_.toString).getOrElse(""),
      "project_language"     -> state.projectLanguage.getOrElse("English")
    )

  def updateState[A](state: ProjectContextState, result: A): ProjectContextState =
    result match
      case hmw: HMWTopResult => state.copy(hmwTopResult = Some(hmw))
      case _                 => state

  def execute(state: ProjectContextState): F[ProjectContextState] =
    runAgent[HMWTopResult](state)

final class Crazy8sTask[F[_]: Async](
    agent: Agent[F],
    promptLoader: PromptLoader[F],
    chatContext: ChatContext[F]
) extends AgentTaskNode[F](agent, promptLoader, chatContext):
  val name       = WorkflowStep.Crazy8s
  val promptName = "Crazy8sTask"

  def templateVars(state: ProjectContextState): Map[String, String] =
    Map(
      "project_requirements" -> state.projectRequirements.map(_.toString).getOrElse(""),
      "empathy_map_result"   -> state.empathyMapResult.map(_.toString).getOrElse(""),
      "project_language"     -> state.projectLanguage.getOrElse("English")
    )

  def updateState[A](state: ProjectContextState, result: A): ProjectContextState =
    result match
      case c8: Crazy8sResult => state.copy(crazy8sResult = Some(c8))
      case _                 => state

  def execute(state: ProjectContextState): F[ProjectContextState] =
    runAgent[Crazy8sResult](state)

final class ScamperTask[F[_]: Async](
    agent: Agent[F],
    promptLoader: PromptLoader[F],
    chatContext: ChatContext[F]
) extends AgentTaskNode[F](agent, promptLoader, chatContext):
  val name       = WorkflowStep.Scamper
  val promptName = "ScamperTask"

  def templateVars(state: ProjectContextState): Map[String, String] =
    Map(
      "project_requirements" -> state.projectRequirements.map(_.toString).getOrElse(""),
      "empathy_map_result"   -> state.empathyMapResult.map(_.toString).getOrElse(""),
      "weird_problems"       -> state.weirdProblems.map(_.toString).getOrElse(""),
      "project_language"     -> state.projectLanguage.getOrElse("English")
    )

  def updateState[A](state: ProjectContextState, result: A): ProjectContextState =
    result match
      case sc: ScamperResult => state.copy(scamperResult = Some(sc))
      case _                 => state

  def execute(state: ProjectContextState): F[ProjectContextState] =
    runAgent[ScamperResult](state)

final class PremiumReportTask[F[_]: Async](
    agent: Agent[F],
    promptLoader: PromptLoader[F],
    chatContext: ChatContext[F]
) extends AgentTaskNode[F](agent, promptLoader, chatContext):
  val name       = WorkflowStep.PremiumReport
  val promptName = "PremiumReportTask"

  def templateVars(state: ProjectContextState): Map[String, String] =
    Map(
      "project_requirements" -> state.projectRequirements.map(_.toString).getOrElse(""),
      "analysis_results"     -> state.competitiveAnalysisResult.map(_.toString).getOrElse(""),
      "project_language"     -> state.projectLanguage.getOrElse("English")
    )

  def updateState[A](state: ProjectContextState, result: A): ProjectContextState =
    result match
      case pr: PremiumReportResult => state.copy(premiumReportResult = Some(pr))
      case _                       => state

  def execute(state: ProjectContextState): F[ProjectContextState] =
    runAgent[PremiumReportResult](state)

final class TrendAnalysisTask[F[_]: Async](
    agent: Agent[F],
    promptLoader: PromptLoader[F],
    chatContext: ChatContext[F],
    searchTool: AgentTool[F]
) extends TaskNode[F]:
  val name = WorkflowStep.TrendAnalysis

  def execute(state: ProjectContextState): F[ProjectContextState] =
    val problems = state.reformulatedProblems.map(_.problems).getOrElse(Nil)
    for
      _  <- chatContext.sendState(s"Analyzing trends for ${problems.length} problems...")
      pt <- promptLoader.loadTaskPrompt("TrendAnalysisTask")
      baseVars = Map(
        "project_requirements" -> state.projectRequirements.map(_.toString).getOrElse(""),
        "project_language"     -> state.projectLanguage.getOrElse("English")
      )
      trends <- problems.zipWithIndex.traverse { case (problem, idx) =>
        val vars   = baseVars ++ Map("problem" -> problem.toString)
        val prompt = pt.render(vars)
        Async[F].delay(scribe.info(s"[TrendAnalysis] problem ${idx + 1}/${problems.length}: ${problem.problemId}")) *>
          agent.tooledCall[ProblemTrend](prompt, List(searchTool), 3).flatMap {
            case AgentResult.Success(v, _) => Async[F].pure(v.copy(problemId = problem.problemId))
            case AgentResult.Failure(m, _) =>
              Async[F].raiseError(sding.domain.AppError.AgentError.LlmInvocationFailed(name, m))
          }
      }
      (nextState, _) = state.incrementIteration(name)
    yield nextState.copy(problemTrends = Some(ProblemsTrends(trends)))

final class ProblemSelectionTask[F[_]: Async](
    chatContext: ChatContext[F]
) extends TaskNode[F]:
  val name = WorkflowStep.ProblemSelection

  def execute(state: ProjectContextState): F[ProjectContextState] =
    chatContext.sendState("Scoring and ranking problems...") *> Async[F].delay {
      val trends   = state.problemTrends.map(_.trends).getOrElse(Nil)
      val problems = state.reformulatedProblems.map(_.problems).getOrElse(Nil)

      val matrix = trends.map { trend =>
        val totalPages          = trend.pages.length
        val pagesWithComplaints = trend.pages.count(_.hasComplaint)
        val maxIntensity        = if trend.pages.isEmpty then 0 else trend.pages.map(_.complaintIntensity).max

        val evidenceStrengthScore  = Math.min(10, totalPages * 2)
        val problemValidationScore = if pagesWithComplaints > 0 then Math.min(10, pagesWithComplaints * 3) else 1
        val marketSignalScore      = Math.min(10, maxIntensity * 2)
        val overall                = (evidenceStrengthScore + problemValidationScore + marketSignalScore) / 3.0

        val matchingProblem = problems.find(_.problemId == trend.problemId)
        val insights        = trend.pages.filter(_.hasComplaint).map(_.keySnippet).take(3)

        ProblemScoreMatrix(
          problemId = trend.problemId,
          evidenceScore = EvidenceScore(
            evidenceStrengthScore = evidenceStrengthScore,
            problemValidationScore = problemValidationScore,
            marketSignalScore = marketSignalScore,
            overallScore = overall,
            evidenceExplanation = s"Found $totalPages pages with relevant content",
            validationExplanation = s"$pagesWithComplaints pages contain user complaints",
            marketExplanation = s"Highest complaint intensity: $maxIntensity",
            decisionInsight = matchingProblem.map(_.statement).getOrElse("N/A")
          ),
          totalPagesFound = totalPages,
          pagesWithComplaints = pagesWithComplaints,
          highestComplaintIntensity = maxIntensity,
          evidenceStrength = trend.evidenceStrength,
          keyInsights = insights
        )
      }

      val sorted         = matrix.sortBy(-_.evidenceScore.overallScore)
      val guidance       = if sorted.nonEmpty then Some(s"Top problem: ID ${sorted.head.problemId}") else None
      val (nextState, _) = state.incrementIteration(name)
      nextState.copy(problemsSelected = Some(ProblemsSelectedResult(sorted, guidance)))
    }

final class CompetitiveAnalysisTask[F[_]: Async](
    agent: Agent[F],
    promptLoader: PromptLoader[F],
    chatContext: ChatContext[F],
    searchTool: AgentTool[F]
) extends TaskNode[F]:
  val name = WorkflowStep.CompetitiveAnalysis

  def execute(state: ProjectContextState): F[ProjectContextState] =
    val variants = state.scamperResult.map(_.scamperVariants).getOrElse(Nil)
    for
      _  <- chatContext.sendState(s"Running competitive analysis for ${variants.length} variants...")
      pt <- promptLoader.loadTaskPrompt("CompetitiveAnalysisTask")
      baseVars = Map(
        "project_requirements" -> state.projectRequirements.map(_.toString).getOrElse(""),
        "empathy_map_result"   -> state.empathyMapResult.map(_.toString).getOrElse(""),
        "project_language"     -> state.projectLanguage.getOrElse("English")
      )
      entries <- variants.traverse { variant =>
        val vars   = baseVars ++ Map("scamper_variant" -> variant.toString)
        val prompt = pt.render(vars)
        agent.tooledCall[CompetitiveAnalysisEntry](prompt, List(searchTool), 3).flatMap {
          case AgentResult.Success(v, _) => Async[F].pure(v)
          case AgentResult.Failure(m, _) =>
            Async[F].raiseError(sding.domain.AppError.AgentError.LlmInvocationFailed(name, m))
        }
      }
      (nextState, _) = state.incrementIteration(name)
    yield nextState.copy(competitiveAnalysisResult = Some(CompetitiveAnalysisResult(entries)))

final class HumanRequirementsTask[F[_]: Async](
    chatContext: ChatContext[F]
) extends TaskNode[F]:
  val name = WorkflowStep.HumanRequirements

  def execute(state: ProjectContextState): F[ProjectContextState] =
    for
      _            <- chatContext.sendMessage("Please provide your project requirements.")
      projectType  <- chatContext.requestInput(Choice("Market type?", List("B2B", "B2C", "C2C", "C2B")))
      softwareType <- chatContext.requestInput(
        Choice("Software type?", List("Web SaaS", "Software", "Mobile App", "Hardware", "Other"))
      )
      problem <- chatContext.requestInput(FreeText("Describe the problem you want to solve:"))
    yield state.copy(
      projectRequirements = Some(
        ProjectRequirements(
          projectType = projectType,
          projectSoftwareType = softwareType,
          projectProblem = Some(problem)
        )
      )
    )

final class MarkdownGenerationTask[F[_]: Async]() extends TaskNode[F]:
  val name = WorkflowStep.MarkdownGeneration

  def execute(state: ProjectContextState): F[ProjectContextState] =
    Async[F].pure {
      val report  = state.premiumReportResult.map(_.premiumReport)
      val title   = report.map(_.selectedProjectDetails.projectTitle).getOrElse("Project Report")
      val content = report
        .map(r => s"# ${r.selectedProjectDetails.projectTitle}\n\n## Executive Summary\n\n${r.executiveSummary}")
        .getOrElse("No report data available.")
      state.copy(
        markdownGenerationResult = Some(MarkdownGenerationResult(content, title)),
        isFinalNode = true
      )
    }
