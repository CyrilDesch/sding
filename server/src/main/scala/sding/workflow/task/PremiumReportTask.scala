package sding.workflow.task

import cats.effect.Async
import chat4s.ai.Agent
import chat4s.ai.JsonSchemaOf
import chat4s.ai.prompt.PromptLoader
import chat4s.io.ChatContext
import io.circe.Decoder
import io.circe.Encoder
import sding.protocol.WorkflowStep
import sding.workflow.AgentTaskNode
import sding.workflow.state.ProjectContextState

final case class ProblemContext(
    problemStatement: String,
    targetUsers: List[String],
    userPainPoints: List[String],
    problemScope: String,
    problemUrgency: String
) derives Decoder,
      Encoder.AsObject,
      JsonSchemaOf
final case class MarketContext(
    marketTrends: List[String],
    technologyEnablers: List[String],
    competitiveGaps: List[String],
    marketOpportunitySize: String,
    timingRationale: String
) derives Decoder,
      Encoder.AsObject,
      JsonSchemaOf
final case class StrategicRationale(
    businessAlignment: String,
    uniqueValueProposition: String,
    competitiveAdvantages: List[String],
    strategicImportance: String,
    successMetrics: List[String]
) derives Decoder,
      Encoder.AsObject,
      JsonSchemaOf
final case class ValidationEvidence(
    userResearchInsights: List[String],
    marketValidation: List[String],
    prototypeLearnings: List[String],
    competitiveAnalysisFindings: List[String],
    riskMitigation: List[String]
) derives Decoder,
      Encoder.AsObject,
      JsonSchemaOf
final case class ExecutionReadiness(
    teamCapabilities: List[String],
    resourceRequirements: String,
    developmentApproach: String,
    goToMarketStrategy: String,
    nextSteps: List[String]
) derives Decoder,
      Encoder.AsObject,
      JsonSchemaOf
final case class AlternativeExploration(
    problemsConsidered: List[String],
    selectionRationale: String,
    solutionVariants: List[String],
    keyDecisionPoints: List[String]
) derives Decoder,
      Encoder.AsObject,
      JsonSchemaOf
final case class SelectedProjectDetails(
    projectTitle: String,
    coreValueProposition: String,
    targetUserPersonas: List[String],
    keyFeatures: List[String],
    successMetrics: List[String],
    implementationPriority: String
) derives Decoder,
      Encoder.AsObject,
      JsonSchemaOf
final case class PremiumReportSchema(
    executiveSummary: String,
    problemContext: ProblemContext,
    marketContext: MarketContext,
    strategicRationale: StrategicRationale,
    validationEvidence: ValidationEvidence,
    executionReadiness: ExecutionReadiness,
    alternativeExploration: AlternativeExploration,
    selectedProjectDetails: SelectedProjectDetails,
    methodologyUsed: List[String],
    dataSources: List[String],
    recommendation: String,
    confidenceLevel: String
) derives Decoder,
      Encoder.AsObject,
      JsonSchemaOf
final case class PremiumReportResult(premiumReport: PremiumReportSchema, reportMetadata: Map[String, String])
    derives Decoder,
      Encoder.AsObject,
      JsonSchemaOf

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
