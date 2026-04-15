package chat4s.graph

/** F-free, S-free structural description of a workflow.
  *
  * Captures the graph topology without execution concerns — suitable for
  * documentation, diagram export, and static analysis.
  */
final case class WorkflowBlueprint(
    name: String,
    nodes: List[String],
    edges: List[WorkflowEdge],
    conditionalEdges: List[ConditionalBlueprintEdge],
    entryPoint: String
)

/** Conditional routing edge stripped of its state-dependent router function.
  * Only the reachable targets are preserved.
  */
final case class ConditionalBlueprintEdge(from: String, targets: Map[String, String])

object WorkflowBlueprint:
  def from[F[_], S](name: String, workflow: WorkflowDef[F, S]): WorkflowBlueprint =
    WorkflowBlueprint(
      name = name,
      nodes = workflow.nodes.keys.toList.sorted,
      edges = workflow.edges,
      conditionalEdges = workflow.conditionalEdges.map(ce => ConditionalBlueprintEdge(ce.from, ce.targets)),
      entryPoint = workflow.entryPoint
    )
