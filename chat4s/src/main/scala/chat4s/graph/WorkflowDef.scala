package chat4s.graph

final case class WorkflowEdge(from: String, to: String)

final case class ConditionalWorkflowEdge[S](
    from: String,
    router: S => String,
    targets: Map[String, String]
)

final case class WorkflowDef[F[_], S](
    nodes: Map[String, Step[F, S]],
    edges: List[WorkflowEdge],
    conditionalEdges: List[ConditionalWorkflowEdge[S]],
    entryPoint: String
):
  def resolveNext(nodeId: String, state: S): Option[String] =
    conditionalEdges.find(_.from == nodeId) match
      case Some(ce) => ce.targets.get(ce.router(state))
      case None     => edges.find(_.from == nodeId).map(_.to)
