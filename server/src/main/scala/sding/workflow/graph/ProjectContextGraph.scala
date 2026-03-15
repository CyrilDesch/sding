package sding.workflow.graph

import cats.effect.Async
import cats.effect.Ref
import cats.syntax.all.*
import sding.workflow.state.ProjectContextState
import sding.workflow.task.TaskNode

final case class GraphEdge(from: String, to: String)
final case class ConditionalEdge(from: String, router: ProjectContextState => String, targets: Map[String, String])

final case class WorkflowGraph[F[_]](
    nodes: Map[String, TaskNode[F]],
    edges: List[GraphEdge],
    conditionalEdges: List[ConditionalEdge],
    entryPoint: String
)

object ProjectContextGraph:

  def build[F[_]](tasks: Map[String, TaskNode[F]]): WorkflowGraph[F] =
    val edges = List(
      GraphEdge("human_requirements", "weird_problem_generation"),
      GraphEdge("weird_problem_generation", "problem_reformulation"),
      GraphEdge("problem_reformulation", "trend_analysis"),
      GraphEdge("trend_analysis", "problem_selection"),
      GraphEdge("problem_selection", "human_problem_selection"),
      GraphEdge("user_interviews", "empathy_map"),
      GraphEdge("empathy_map", "jtbd_definition"),
      GraphEdge("jtbd_definition", "human_jtbd_selection"),
      GraphEdge("hmw", "crazy8s"),
      GraphEdge("crazy8s", "scamper"),
      GraphEdge("scamper", "competitive_analysis"),
      GraphEdge("competitive_analysis", "human_comprehensive_selection"),
      GraphEdge("prototype_builds", "human_project_selection"),
      GraphEdge("premium_report", "markdown_generation")
    )

    val conditionalEdges = List(
      ConditionalEdge(
        "human_problem_selection",
        s =>
          if s.hasFeedback("desired_modification") then "loop_back"
          else "proceed",
        Map(
          "loop_back" -> "weird_problem_generation",
          "proceed"   -> "user_interviews"
        )
      ),
      ConditionalEdge(
        "human_jtbd_selection",
        s =>
          if s.hasFeedback("jtbd_improvement_feedback") then "loop_back"
          else "proceed",
        Map(
          "loop_back" -> "user_interviews",
          "proceed"   -> "hmw"
        )
      ),
      ConditionalEdge(
        "human_comprehensive_selection",
        s =>
          if s.hasFeedback("comprehensive_analysis_feedback") then "loop_back"
          else "proceed",
        Map(
          "loop_back" -> "hmw",
          "proceed"   -> "prototype_builds"
        )
      ),
      ConditionalEdge(
        "human_project_selection",
        s =>
          if s.hasFeedback("human_project_revision_feedback") then "loop_back"
          else "proceed",
        Map(
          "loop_back" -> "prototype_builds",
          "proceed"   -> "premium_report"
        )
      )
    )

    WorkflowGraph(
      nodes = tasks,
      edges = edges,
      conditionalEdges = conditionalEdges,
      entryPoint = "human_requirements"
    )

  def execute[F[_]: Async](
      graph: WorkflowGraph[F],
      initialState: ProjectContextState
  ): fs2.Stream[F, (String, ProjectContextState)] =
    fs2.Stream.eval(Ref.of[F, ProjectContextState](initialState)).flatMap { stateRef =>
      def resolveNext(currentNode: String, state: ProjectContextState): Option[String] =
        graph.conditionalEdges.find(_.from == currentNode) match
          case Some(ce) =>
            val route = ce.router(state)
            ce.targets.get(route)
          case None =>
            graph.edges.find(_.from == currentNode).map(_.to)

      def step(nodeName: String): fs2.Stream[F, (String, ProjectContextState)] =
        graph.nodes.get(nodeName) match
          case None       => fs2.Stream.empty
          case Some(task) =>
            fs2.Stream
              .eval {
                stateRef.get.flatMap(task.execute).flatTap(stateRef.set)
              }
              .map(s => (nodeName, s))
              .flatMap { pair =>
                val next = resolveNext(nodeName, pair._2)
                fs2.Stream.emit(pair) ++ next.fold(fs2.Stream.empty.covaryAll[F, (String, ProjectContextState)])(
                  step
                )
              }

      step(graph.entryPoint)
    }
