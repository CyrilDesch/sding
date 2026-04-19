package chat4s.graph

object WorkflowMermaid:

  private def sanitizeId(id: String): String =
    id.replaceAll("[^a-zA-Z0-9_]", "_")

  def render(blueprint: WorkflowBlueprint): String =
    val startLines = List(
      "flowchart TD",
      "  __start__((start))",
      s"  __start__ --> ${sanitizeId(blueprint.entryPoint)}"
    )
    val nodeLines = blueprint.nodes.sorted.map { id =>
      s"""  ${sanitizeId(id)}["${id}"]"""
    }
    val edgeLines = blueprint.edges.map { edge =>
      s"  ${sanitizeId(edge.from)} --> ${sanitizeId(edge.to)}"
    }
    val conditionalEdgeLines = blueprint.conditionalEdges.flatMap { ce =>
      ce.targets.toList.sortBy(_._1).map { (routeKey, targetId) =>
        s"""  ${sanitizeId(ce.from)} -->|"${routeKey}"| ${sanitizeId(targetId)}"""
      }
    }
    (startLines ++ nodeLines ++ edgeLines ++ conditionalEdgeLines).mkString("\n")

  def render[F[_], S](workflow: WorkflowDef[F, S]): String =
    render(WorkflowBlueprint.from("", workflow))

  extension [F[_], S](workflow: WorkflowDef[F, S]) def toMermaid: String = render(workflow)
