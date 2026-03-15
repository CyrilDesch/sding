package sding.protocol

import io.circe.Decoder
import io.circe.Encoder

enum WorkflowStep(val snakeName: String, val friendlyName: String):
  case HumanRequirements           extends WorkflowStep("human_requirements", "Your Requirements")
  case WeirdProblemGeneration      extends WorkflowStep("weird_problem_generation", "Problem Discovery")
  case ProblemReformulation        extends WorkflowStep("problem_reformulation", "Problem Refinement")
  case TrendAnalysis               extends WorkflowStep("trend_analysis", "Trend Analysis")
  case ProblemSelection            extends WorkflowStep("problem_selection", "Problem Selection")
  case HumanProblemSelection       extends WorkflowStep("human_problem_selection", "Choose a Problem")
  case UserInterviews              extends WorkflowStep("user_interviews", "User Interviews")
  case EmpathyMap                  extends WorkflowStep("empathy_map", "Empathy Map")
  case JtbdDefinition              extends WorkflowStep("jtbd_definition", "Jobs to Be Done")
  case HumanJtbdSelection          extends WorkflowStep("human_jtbd_selection", "Choose Key Job")
  case Hmw                         extends WorkflowStep("hmw", "How Might We?")
  case Crazy8s                     extends WorkflowStep("crazy8s", "Crazy 8s Ideas")
  case Scamper                     extends WorkflowStep("scamper", "SCAMPER Analysis")
  case CompetitiveAnalysis         extends WorkflowStep("competitive_analysis", "Competitive Analysis")
  case HumanComprehensiveSelection extends WorkflowStep("human_comprehensive_selection", "Select Best Idea")
  case PrototypeBuilds             extends WorkflowStep("prototype_builds", "Build Prototypes")
  case HumanProjectSelection       extends WorkflowStep("human_project_selection", "Choose Your Project")
  case PremiumReport               extends WorkflowStep("premium_report", "Premium Report")
  case MarkdownGeneration          extends WorkflowStep("markdown_generation", "Finalize Report")

object WorkflowStep:
  private val bySnakeName: Map[String, WorkflowStep] =
    values.map(s => s.snakeName -> s).toMap

  def fromString(name: String): Option[WorkflowStep] =
    bySnakeName.get(name)

  given Encoder[WorkflowStep] = Encoder[String].contramap(_.snakeName)
  given Decoder[WorkflowStep] = Decoder[String].emap(s => fromString(s).toRight(s"Unknown workflow step: $s"))
