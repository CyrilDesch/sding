package sding.workflow.result

import io.circe.Decoder
import io.circe.Encoder

final case class ProjectRequirements(
    projectType: String,
    projectSoftwareType: String,
    projectProblem: Option[String] = None,
    projectDomain: Option[String] = None,
    projectPromiseStatement: Option[String] = None
) derives Decoder,
      Encoder.AsObject

final case class WeirdProblem(id: Int, statement: String, fullStatement: String) derives Decoder, Encoder.AsObject
final case class WeirdProblems(problems: List[WeirdProblem]) derives Decoder, Encoder.AsObject

final case class ReformulatedProblem(
    problemId: Int,
    statement: String,
    targetAudience: String,
    evidenceSnippet: String,
    situation: String,
    impactMetric: String,
    jobToBeDone: String
) derives Decoder,
      Encoder.AsObject
final case class ReformulatedProblems(problems: List[ReformulatedProblem]) derives Decoder, Encoder.AsObject

final case class Page(title: String, hasComplaint: Boolean, complaintIntensity: Int, keySnippet: String)
    derives Decoder,
      Encoder.AsObject
final case class ProblemTrend(problemId: Int, pages: List[Page], complaintCoverage: Int, evidenceStrength: String)
    derives Decoder,
      Encoder.AsObject
final case class ProblemsTrends(trends: List[ProblemTrend]) derives Decoder, Encoder.AsObject

final case class EvidenceScore(
    evidenceStrengthScore: Int,
    problemValidationScore: Int,
    marketSignalScore: Int,
    overallScore: Double,
    evidenceExplanation: String,
    validationExplanation: String,
    marketExplanation: String,
    decisionInsight: String
) derives Decoder,
      Encoder.AsObject
final case class ProblemScoreMatrix(
    problemId: Int,
    evidenceScore: EvidenceScore,
    totalPagesFound: Int,
    pagesWithComplaints: Int,
    highestComplaintIntensity: Int,
    evidenceStrength: String,
    keyInsights: List[String]
) derives Decoder,
      Encoder.AsObject
final case class ProblemsSelectedResult(
    decisionMatrix: List[ProblemScoreMatrix],
    selectionGuidance: Option[String] = None
) derives Decoder,
      Encoder.AsObject

final case class Interview(
    keyQuotes: List[String],
    painSeverity: Int,
    currentSolutions: List[String],
    willingnessToPay: Int
) derives Decoder,
      Encoder.AsObject
final case class UserInterviewResult(interviews: List[Interview]) derives Decoder, Encoder.AsObject

final case class EmpathyMap(
    personaDescription: String,
    see: List[String],
    hear: List[String],
    think: List[String],
    feel: List[String],
    pains: List[String],
    desiredOutcomes: List[String],
    insights: List[String]
) derives Decoder,
      Encoder.AsObject
final case class EmpathyMapResult(empathyMap: EmpathyMap) derives Decoder, Encoder.AsObject

final case class JobToBeDone(
    id: Int,
    jobStatement: String,
    importance: Int,
    satisfactionToday: Int,
    archetypeLabel: String
) derives Decoder,
      Encoder.AsObject
final case class JTBDDefinitionResult(jobs: List[JobToBeDone]) derives Decoder, Encoder.AsObject

final case class HMWQuestion(
    id: Int,
    question: String,
    impact: Int,
    effort: Int,
    viralLoopStrength: Int,
    jobsId: Int
) derives Decoder,
      Encoder.AsObject
final case class HMWTopResult(topQuestions: List[HMWQuestion]) derives Decoder, Encoder.AsObject

final case class SketchVariant(hmwId: Int, sketchDescriptions: List[String]) derives Decoder, Encoder.AsObject
final case class Crazy8sResult(variants: List[SketchVariant]) derives Decoder, Encoder.AsObject

final case class ScamperVariant(
    id: Int,
    hmwId: Int,
    substitute: String,
    combine: String,
    adapt: String,
    modify: String,
    putToAnotherUse: String,
    eliminate: String,
    reverse: String,
    feasibilityScore: Int
) derives Decoder,
      Encoder.AsObject
final case class ScamperResult(scamperVariants: List[ScamperVariant]) derives Decoder, Encoder.AsObject

final case class CompetitiveAnalysisEntry(
    scamperId: Int,
    competitorOfferings: List[String],
    competitiveAdvantages: List[String],
    distributionAdvantage: List[String],
    opportunity: List[String],
    advantageScore: Int
) derives Decoder,
      Encoder.AsObject
final case class CompetitiveAnalysisResult(vpCompetitiveAnalysis: List[CompetitiveAnalysisEntry])
    derives Decoder,
      Encoder.AsObject

final case class StoryboardStep(stepNumber: Int, description: String, justification: String)
    derives Decoder,
      Encoder.AsObject
final case class Storyboard(scamperId: Int, steps: List[StoryboardStep]) derives Decoder, Encoder.AsObject
final case class PrototypeBuildResult(storyboards: List[Storyboard]) derives Decoder, Encoder.AsObject

final case class StructuredFeedback(
    usabilityHurdle: String,
    expectedOutcome: String,
    suggestedUiChange: String,
    forWhatCanPay: String,
    forWhatCanRecommend: String
) derives Decoder,
      Encoder.AsObject
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
      Encoder.AsObject
final case class UserTestsResult(userTests: List[UserTest]) derives Decoder, Encoder.AsObject

final case class FiveWs(who: String, what: String, why: String, where: String, when: String)
    derives Decoder,
      Encoder.AsObject
final case class Chip(text: String, explanation: String) derives Decoder, Encoder.AsObject
final case class Metric(value: String, label: String) derives Decoder, Encoder.AsObject
final case class MVPFeature(name: String, description: String, priority: String) derives Decoder, Encoder.AsObject
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
      Encoder.AsObject
final case class SynthetizeProjectsResult(projectsCards: List[ProjectCardSchema]) derives Decoder, Encoder.AsObject

final case class ProblemContext(
    problemStatement: String,
    targetUsers: List[String],
    userPainPoints: List[String],
    problemScope: String,
    problemUrgency: String
) derives Decoder,
      Encoder.AsObject
final case class MarketContext(
    marketTrends: List[String],
    technologyEnablers: List[String],
    competitiveGaps: List[String],
    marketOpportunitySize: String,
    timingRationale: String
) derives Decoder,
      Encoder.AsObject
final case class StrategicRationale(
    businessAlignment: String,
    uniqueValueProposition: String,
    competitiveAdvantages: List[String],
    strategicImportance: String,
    successMetrics: List[String]
) derives Decoder,
      Encoder.AsObject
final case class ValidationEvidence(
    userResearchInsights: List[String],
    marketValidation: List[String],
    prototypeLearnings: List[String],
    competitiveAnalysisFindings: List[String],
    riskMitigation: List[String]
) derives Decoder,
      Encoder.AsObject
final case class ExecutionReadiness(
    teamCapabilities: List[String],
    resourceRequirements: String,
    developmentApproach: String,
    goToMarketStrategy: String,
    nextSteps: List[String]
) derives Decoder,
      Encoder.AsObject
final case class AlternativeExploration(
    problemsConsidered: List[String],
    selectionRationale: String,
    solutionVariants: List[String],
    keyDecisionPoints: List[String]
) derives Decoder,
      Encoder.AsObject
final case class SelectedProjectDetails(
    projectTitle: String,
    coreValueProposition: String,
    targetUserPersonas: List[String],
    keyFeatures: List[String],
    successMetrics: List[String],
    implementationPriority: String
) derives Decoder,
      Encoder.AsObject
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
      Encoder.AsObject
final case class PremiumReportResult(premiumReport: PremiumReportSchema, reportMetadata: Map[String, String])
    derives Decoder,
      Encoder.AsObject

final case class MarkdownGenerationResult(markdownContent: String, reportTitle: String)
    derives Decoder,
      Encoder.AsObject

final case class HumanGateResult(selectedProblemId: Int) derives Decoder, Encoder.AsObject
final case class HumanJTBDSelectionResult(selectedJtbdId: Int) derives Decoder, Encoder.AsObject
final case class HumanComprehensiveSelectionResult(selectedVariantId: Option[Int] = None)
    derives Decoder,
      Encoder.AsObject
final case class HumanProjectSelectionResult(selectedProjectId: Int) derives Decoder, Encoder.AsObject
