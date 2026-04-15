package chat4s.graph

import cats.effect.IO
import chat4s.graph.WorkflowMermaid.toMermaid
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class WorkflowMermaidSpec extends AnyWordSpec with Matchers:

  private def step(nodeId: String): Step[IO, Int] =
    new Step[IO, Int]:
      def id                           = nodeId
      def execute(state: Int): IO[Int] = IO.pure(state)

  "WorkflowMermaid.render" should {

    "produce a flowchart header with a start node wired to the entry point" in {
      val wf = WorkflowDef[IO, Int](
        nodes = Map("a" -> step("a")),
        edges = Nil,
        conditionalEdges = Nil,
        entryPoint = "a"
      )
      val diagram = wf.toMermaid
      diagram should include("flowchart TD")
      diagram should include("__start__((start))")
      diagram should include("__start__ --> a")
    }

    "declare every node with its label" in {
      val wf = WorkflowDef[IO, Int](
        nodes = Map("a" -> step("a"), "b" -> step("b"), "c" -> step("c")),
        edges = List(WorkflowEdge("a", "b"), WorkflowEdge("b", "c")),
        conditionalEdges = Nil,
        entryPoint = "a"
      )
      val diagram = wf.toMermaid
      diagram should include("""a["a"]""")
      diagram should include("""b["b"]""")
      diagram should include("""c["c"]""")
    }

    "render static edges" in {
      val wf = WorkflowDef[IO, Int](
        nodes = Map("a" -> step("a"), "b" -> step("b"), "c" -> step("c")),
        edges = List(WorkflowEdge("a", "b"), WorkflowEdge("b", "c")),
        conditionalEdges = Nil,
        entryPoint = "a"
      )
      val diagram = wf.toMermaid
      diagram should include("a --> b")
      diagram should include("b --> c")
    }

    "render conditional edges with route key labels" in {
      val wf = WorkflowDef[IO, Int](
        nodes = Map("gate" -> step("gate"), "left" -> step("left"), "right" -> step("right")),
        edges = Nil,
        conditionalEdges = List(
          ConditionalWorkflowEdge(
            "gate",
            s => if s > 5 then "go_left" else "go_right",
            Map("go_left" -> "left", "go_right" -> "right")
          )
        ),
        entryPoint = "gate"
      )
      val diagram = wf.toMermaid
      diagram should include("""gate -->|"go_left"| left""")
      diagram should include("""gate -->|"go_right"| right""")
    }

    "sanitize special characters in node IDs while preserving the display label" in {
      val wf = WorkflowDef[IO, Int](
        nodes = Map("step-one" -> step("step-one"), "step-two" -> step("step-two")),
        edges = List(WorkflowEdge("step-one", "step-two")),
        conditionalEdges = Nil,
        entryPoint = "step-one"
      )
      val diagram = wf.toMermaid
      diagram should include("__start__ --> step_one")
      diagram should include("""step_one["step-one"]""")
      diagram should include("step_one --> step_two")
    }
  }
