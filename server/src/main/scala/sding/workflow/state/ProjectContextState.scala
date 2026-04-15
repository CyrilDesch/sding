package sding.workflow.state

import io.circe.Decoder
import io.circe.Encoder
import sding.protocol.WorkflowStep
import sding.workflow.task.*

final case class ProjectContextState(
    workflowId: String = java.util.UUID.randomUUID().toString,
    workflowName: String = "",
    chatIdStr: Option[String] = None,
    errorMessage: Option[String] = None,
    goBackFeedbacks: Map[String, List[String]] = Map.empty,
    iterationCount: Map[String, Int] = Map.empty,
    projectLanguage: Option[String] = None,
    projectLanguageIso: Option[String] = None,
    finalDocument: Option[String] = None,
    documentPdfPath: Option[String] = None,
    isFinalNode: Boolean = false,
    projectRequirements: Option[ProjectRequirements] = None,
    weirdProblems: Option[WeirdProblems] = None,
    reformulatedProblems: Option[ReformulatedProblems] = None,
    problemTrends: Option[ProblemsTrends] = None,
    problemsSelected: Option[ProblemsSelectedResult] = None,
    humanProblemSelectionResult: Option[HumanGateResult] = None,
    userInterviews: Option[UserInterviewResult] = None,
    empathyMapResult: Option[EmpathyMapResult] = None,
    jtbdDefinitionResult: Option[JTBDDefinitionResult] = None,
    humanJtbdSelectionResult: Option[HumanJTBDSelectionResult] = None,
    hmwTopResult: Option[HMWTopResult] = None,
    crazy8sResult: Option[Crazy8sResult] = None,
    scamperResult: Option[ScamperResult] = None,
    competitiveAnalysisResult: Option[CompetitiveAnalysisResult] = None,
    humanComprehensiveSelectionResult: Option[HumanComprehensiveSelectionResult] = None,
    prototypeBuildResult: Option[PrototypeBuildResult] = None,
    userTestResult: Option[UserTestsResult] = None,
    synthetizeProjectResult: Option[SynthetizeProjectsResult] = None,
    humanProjectSelectionResult: Option[HumanProjectSelectionResult] = None,
    premiumReportResult: Option[PremiumReportResult] = None,
    markdownGenerationResult: Option[MarkdownGenerationResult] = None
) derives Decoder,
      Encoder.AsObject:

  def incrementIteration(step: WorkflowStep): (ProjectContextState, Int) =
    val current = iterationCount.getOrElse(step.snakeName, 0) + 1
    (copy(iterationCount = iterationCount.updated(step.snakeName, current)), current)

  def hasFeedback(key: String): Boolean =
    goBackFeedbacks.get(key).exists(_.nonEmpty)

  def appendFeedback(key: String, feedback: String): ProjectContextState =
    val existing = goBackFeedbacks.getOrElse(key, List.empty)
    copy(goBackFeedbacks = goBackFeedbacks.updated(key, existing :+ feedback))

  def clearFeedback(key: String): ProjectContextState =
    copy(goBackFeedbacks = goBackFeedbacks - key)
