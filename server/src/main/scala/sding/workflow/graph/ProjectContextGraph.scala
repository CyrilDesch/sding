package sding.workflow.graph

import cats.effect.Async
import cats.effect.Ref
import cats.syntax.all.*
import sding.protocol.WorkflowStep
import sding.workflow.state.ProjectContextState
import sding.workflow.task.TaskNode

final case class GraphEdge(from: WorkflowStep, to: WorkflowStep)
final case class ConditionalEdge(
    from: WorkflowStep,
    router: ProjectContextState => String,
    targets: Map[String, WorkflowStep]
)

final case class WorkflowGraph[F[_]](
    nodes: Map[WorkflowStep, TaskNode[F]],
    edges: List[GraphEdge],
    conditionalEdges: List[ConditionalEdge],
    entryPoint: WorkflowStep
)

object ProjectContextGraph:

  def build[F[_]](tasks: Map[WorkflowStep, TaskNode[F]]): WorkflowGraph[F] =
    import WorkflowStep.*

    val edges = List(
      GraphEdge(HumanRequirements, WeirdProblemGeneration),
      GraphEdge(WeirdProblemGeneration, ProblemReformulation),
      GraphEdge(ProblemReformulation, TrendAnalysis),
      GraphEdge(TrendAnalysis, ProblemSelection),
      GraphEdge(ProblemSelection, HumanProblemSelection),
      GraphEdge(UserInterviews, EmpathyMap),
      GraphEdge(EmpathyMap, JtbdDefinition),
      GraphEdge(JtbdDefinition, HumanJtbdSelection),
      GraphEdge(Hmw, Crazy8s),
      GraphEdge(Crazy8s, Scamper),
      GraphEdge(Scamper, CompetitiveAnalysis),
      GraphEdge(CompetitiveAnalysis, HumanComprehensiveSelection),
      GraphEdge(PrototypeBuilds, HumanProjectSelection),
      GraphEdge(PremiumReport, MarkdownGeneration)
    )

    val conditionalEdges = List(
      ConditionalEdge(
        HumanProblemSelection,
        s => if s.hasFeedback("desired_modification") then "loop_back" else "proceed",
        Map("loop_back" -> WeirdProblemGeneration, "proceed" -> UserInterviews)
      ),
      ConditionalEdge(
        HumanJtbdSelection,
        s => if s.hasFeedback("jtbd_improvement_feedback") then "loop_back" else "proceed",
        Map("loop_back" -> UserInterviews, "proceed" -> Hmw)
      ),
      ConditionalEdge(
        HumanComprehensiveSelection,
        s => if s.hasFeedback("comprehensive_analysis_feedback") then "loop_back" else "proceed",
        Map("loop_back" -> Hmw, "proceed" -> PrototypeBuilds)
      ),
      ConditionalEdge(
        HumanProjectSelection,
        s => if s.hasFeedback("human_project_revision_feedback") then "loop_back" else "proceed",
        Map("loop_back" -> PrototypeBuilds, "proceed" -> PremiumReport)
      )
    )

    WorkflowGraph(
      nodes = tasks,
      edges = edges,
      conditionalEdges = conditionalEdges,
      entryPoint = HumanRequirements
    )

  def resolveNextStep[F[_]](
      graph: WorkflowGraph[F],
      current: WorkflowStep,
      state: ProjectContextState
  ): Option[WorkflowStep] =
    graph.conditionalEdges.find(_.from == current) match
      case Some(ce) => ce.targets.get(ce.router(state))
      case None     => graph.edges.find(_.from == current).map(_.to)

  def execute[F[_]: Async](
      graph: WorkflowGraph[F],
      initialState: ProjectContextState,
      startFrom: WorkflowStep = WorkflowStep.HumanRequirements
  ): fs2.Stream[F, (WorkflowStep, ProjectContextState)] =
    fs2.Stream.eval(Ref.of[F, ProjectContextState](initialState)).flatMap { stateRef =>
      def step(node: WorkflowStep): fs2.Stream[F, (WorkflowStep, ProjectContextState)] =
        graph.nodes.get(node) match
          case None       => fs2.Stream.empty
          case Some(task) =>
            fs2.Stream
              .eval(stateRef.get.flatMap(task.execute).flatTap(stateRef.set))
              .map(s => (node, s))
              .flatMap { pair =>
                fs2.Stream.emit(pair) ++ resolveNextStep(graph, pair._1, pair._2).fold(
                  fs2.Stream.empty.covaryAll[F, (WorkflowStep, ProjectContextState)]
                )(step)
              }

      step(startFrom)
    }
