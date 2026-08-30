package sding.workflow.graph

import cats.effect.Async
import chat4s.ai.Agent
import chat4s.ai.AgentTool
import chat4s.ai.prompt.PromptLoader
import chat4s.graph.ConditionalBlueprintEdge
import chat4s.graph.ConditionalWorkflowEdge
import chat4s.graph.WorkflowBlueprint
import chat4s.graph.WorkflowDef
import chat4s.graph.WorkflowEdge
import chat4s.io.ChatContext
import sding.protocol.WorkflowStep
import sding.workflow.TaskNode
import sding.workflow.state.ProjectContextState
import sding.workflow.task.*

object ProjectContextGraph:
  import WorkflowStep.*

  private val staticEdges: List[WorkflowEdge] = List(
    WorkflowEdge(HumanRequirements.snakeName, WeirdProblemGeneration.snakeName),
    WorkflowEdge(WeirdProblemGeneration.snakeName, ProblemReformulation.snakeName),
    WorkflowEdge(ProblemReformulation.snakeName, TrendAnalysis.snakeName),
    WorkflowEdge(TrendAnalysis.snakeName, ProblemSelection.snakeName),
    WorkflowEdge(ProblemSelection.snakeName, HumanProblemSelection.snakeName),
    WorkflowEdge(UserInterviews.snakeName, EmpathyMap.snakeName),
    WorkflowEdge(EmpathyMap.snakeName, JtbdDefinition.snakeName),
    WorkflowEdge(JtbdDefinition.snakeName, HumanJtbdSelection.snakeName),
    WorkflowEdge(Hmw.snakeName, Crazy8s.snakeName),
    WorkflowEdge(Crazy8s.snakeName, Scamper.snakeName),
    WorkflowEdge(Scamper.snakeName, CompetitiveAnalysis.snakeName),
    WorkflowEdge(CompetitiveAnalysis.snakeName, HumanComprehensiveSelection.snakeName),
    WorkflowEdge(PrototypeBuilds.snakeName, HumanProjectSelection.snakeName),
    WorkflowEdge(PremiumReport.snakeName, MarkdownGeneration.snakeName)
  )

  // Each entry pairs the blueprint edge (targets only) with the runtime router function.
  private type ConditionalRoute = (edge: ConditionalBlueprintEdge, router: ProjectContextState => String)

  private val conditionalRoutes: List[ConditionalRoute] = List(
    (
      edge = ConditionalBlueprintEdge(
        HumanProblemSelection.snakeName,
        Map("loop_back" -> WeirdProblemGeneration.snakeName, "proceed" -> UserInterviews.snakeName)
      ),
      router = s => if s.hasFeedback("desired_modification") then "loop_back" else "proceed"
    ),
    (
      edge = ConditionalBlueprintEdge(
        HumanJtbdSelection.snakeName,
        Map("loop_back" -> UserInterviews.snakeName, "proceed" -> Hmw.snakeName)
      ),
      router = s => if s.hasFeedback("jtbd_improvement_feedback") then "loop_back" else "proceed"
    ),
    (
      edge = ConditionalBlueprintEdge(
        HumanComprehensiveSelection.snakeName,
        Map("loop_back" -> Hmw.snakeName, "proceed" -> PrototypeBuilds.snakeName)
      ),
      router = s => if s.hasFeedback("comprehensive_analysis_feedback") then "loop_back" else "proceed"
    ),
    (
      edge = ConditionalBlueprintEdge(
        HumanProjectSelection.snakeName,
        Map("loop_back" -> PrototypeBuilds.snakeName, "proceed" -> PremiumReport.snakeName)
      ),
      router = s => if s.hasFeedback("human_project_revision_feedback") then "loop_back" else "proceed"
    )
  )

  val blueprint: WorkflowBlueprint = WorkflowBlueprint(
    name = "ProjectContextGraph",
    nodes = WorkflowStep.values.map(_.snakeName).toList,
    edges = staticEdges,
    conditionalEdges = conditionalRoutes.map(_.edge),
    entryPoint = HumanRequirements.snakeName
  )

  def build[F[_]: Async](
      agent: Agent[F],
      promptLoader: PromptLoader[F],
      chatCtx: ChatContext[F],
      searchTool: AgentTool[F]
  ): WorkflowDef[F, ProjectContextState] =
    val tasks: List[TaskNode[F]] = List(
      HumanRequirementsTask[F](chatCtx),
      WeirdProblemGenerationTask[F](agent, promptLoader, chatCtx),
      ProblemReformulationTask[F](agent, promptLoader, chatCtx),
      TrendAnalysisTask[F](agent, promptLoader, chatCtx, searchTool),
      ProblemSelectionTask[F](chatCtx),
      HumanProblemSelectionTask[F](chatCtx),
      UserInterviewsTask[F](agent, promptLoader, chatCtx),
      EmpathyMapTask[F](agent, promptLoader, chatCtx),
      JTBDDefinitionTask[F](agent, promptLoader, chatCtx),
      HumanJTBDSelectionTask[F](chatCtx),
      HMWTask[F](agent, promptLoader, chatCtx),
      Crazy8sTask[F](agent, promptLoader, chatCtx),
      ScamperTask[F](agent, promptLoader, chatCtx),
      CompetitiveAnalysisTask[F](agent, promptLoader, chatCtx, searchTool),
      HumanComprehensiveSelectionTask[F](chatCtx),
      PrototypeBuildsTask[F](agent, promptLoader, chatCtx),
      HumanProjectSelectionTask[F](chatCtx),
      PremiumReportTask[F](agent, promptLoader, chatCtx),
      MarkdownGenerationTask[F]()
    )
    build(tasks.map(t => t.name -> t).toMap)

  def build[F[_]](tasks: Map[WorkflowStep, TaskNode[F]]): WorkflowDef[F, ProjectContextState] =
    WorkflowDef(
      nodes = tasks.map((k, v) => k.snakeName -> v),
      edges = staticEdges,
      conditionalEdges = conditionalRoutes.map(r => ConditionalWorkflowEdge(r.edge.from, r.router, r.edge.targets)),
      entryPoint = blueprint.entryPoint
    )
