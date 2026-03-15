package sding.workflow.graph

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import sding.workflow.state.ProjectContextState
import sding.workflow.task.TaskNode

class ProjectContextGraphSpec extends AsyncWordSpec with AsyncIOSpec with Matchers:

  private def passThrough(nodeName: String, mutation: ProjectContextState => ProjectContextState): TaskNode[IO] =
    new TaskNode[IO]:
      val name                                                         = nodeName
      def execute(state: ProjectContextState): IO[ProjectContextState] =
        IO.pure(mutation(state))

  "ProjectContextGraph.build" should {

    "construct a graph with all 19 expected nodes when provided" in {
      val nodeNames = List(
        "human_requirements",
        "weird_problem_generation",
        "problem_reformulation",
        "trend_analysis",
        "problem_selection",
        "human_problem_selection",
        "user_interviews",
        "empathy_map",
        "jtbd_definition",
        "human_jtbd_selection",
        "hmw",
        "crazy8s",
        "scamper",
        "competitive_analysis",
        "human_comprehensive_selection",
        "prototype_builds",
        "human_project_selection",
        "premium_report",
        "markdown_generation"
      )
      val tasks = nodeNames.map(n => n -> passThrough(n, identity)).toMap
      val graph = ProjectContextGraph.build[IO](tasks)

      graph.entryPoint shouldBe "human_requirements"
      graph.edges should have length 14
      graph.conditionalEdges should have length 4
      graph.nodes should have size 19
    }

    "set correct conditional edge routing from human_problem_selection" in {
      val tasks = Map("human_problem_selection" -> passThrough("human_problem_selection", identity))
      val graph = ProjectContextGraph.build[IO](tasks)
      val ce    = graph.conditionalEdges.find(_.from == "human_problem_selection")
      ce shouldBe defined

      val stateNoFeedback = ProjectContextState()
      ce.get.router(stateNoFeedback) shouldBe "proceed"
      ce.get.targets("proceed") shouldBe "user_interviews"

      val stateWithFeedback = stateNoFeedback.appendFeedback("desired_modification", "redo")
      ce.get.router(stateWithFeedback) shouldBe "loop_back"
      ce.get.targets("loop_back") shouldBe "weird_problem_generation"
    }

    "set correct conditional edge routing from human_jtbd_selection" in {
      val tasks = Map("human_jtbd_selection" -> passThrough("human_jtbd_selection", identity))
      val graph = ProjectContextGraph.build[IO](tasks)
      val ce    = graph.conditionalEdges.find(_.from == "human_jtbd_selection")
      ce shouldBe defined

      ce.get.router(ProjectContextState()) shouldBe "proceed"
      ce.get.targets("proceed") shouldBe "hmw"

      val withFeedback = ProjectContextState().appendFeedback("jtbd_improvement_feedback", "improve")
      ce.get.router(withFeedback) shouldBe "loop_back"
      ce.get.targets("loop_back") shouldBe "user_interviews"
    }

    "set correct conditional edge routing from human_comprehensive_selection" in {
      val tasks = Map("human_comprehensive_selection" -> passThrough("human_comprehensive_selection", identity))
      val graph = ProjectContextGraph.build[IO](tasks)
      val ce    = graph.conditionalEdges.find(_.from == "human_comprehensive_selection")
      ce shouldBe defined

      ce.get.router(ProjectContextState()) shouldBe "proceed"
      ce.get.targets("proceed") shouldBe "prototype_builds"

      val withFeedback = ProjectContextState().appendFeedback("comprehensive_analysis_feedback", "refine")
      ce.get.router(withFeedback) shouldBe "loop_back"
      ce.get.targets("loop_back") shouldBe "hmw"
    }

    "set correct conditional edge routing from human_project_selection" in {
      val tasks = Map("human_project_selection" -> passThrough("human_project_selection", identity))
      val graph = ProjectContextGraph.build[IO](tasks)
      val ce    = graph.conditionalEdges.find(_.from == "human_project_selection")
      ce shouldBe defined

      ce.get.router(ProjectContextState()) shouldBe "proceed"
      ce.get.targets("proceed") shouldBe "premium_report"

      val withFeedback = ProjectContextState().appendFeedback("human_project_revision_feedback", "redo")
      ce.get.router(withFeedback) shouldBe "loop_back"
      ce.get.targets("loop_back") shouldBe "prototype_builds"
    }
  }

  "ProjectContextGraph.execute" should {

    "execute a simple linear subpath" in {
      val tasks = Map(
        "human_requirements" -> passThrough(
          "human_requirements",
          _.copy(workflowName = "step1")
        ),
        "weird_problem_generation" -> passThrough(
          "weird_problem_generation",
          _.copy(projectLanguage = Some("English"))
        )
      )
      val graph = ProjectContextGraph.build[IO](tasks)

      ProjectContextGraph
        .execute(graph, ProjectContextState())
        .compile
        .toList
        .asserting { results =>
          results should have length 2
          results.head._1 shouldBe "human_requirements"
          results.head._2.workflowName shouldBe "step1"
          results(1)._1 shouldBe "weird_problem_generation"
          results(1)._2.projectLanguage shouldBe Some("English")
        }
    }

    "follow conditional edges based on feedback presence" in {
      val tasks = Map(
        "human_problem_selection" -> passThrough(
          "human_problem_selection",
          _.appendFeedback("desired_modification", "retry")
        ),
        "weird_problem_generation" -> passThrough(
          "weird_problem_generation",
          _.clearFeedback("desired_modification")
        ),
        "problem_reformulation" -> passThrough(
          "problem_reformulation",
          identity
        ),
        "user_interviews" -> passThrough("user_interviews", identity)
      )
      val graph = ProjectContextGraph.build[IO](tasks).copy(entryPoint = "human_problem_selection")

      ProjectContextGraph
        .execute(graph, ProjectContextState())
        .compile
        .toList
        .asserting { results =>
          results.map(_._1) should contain("weird_problem_generation")
        }
    }

    "follow proceed path when no feedback present" in {
      val tasks = Map(
        "human_problem_selection" -> passThrough(
          "human_problem_selection",
          identity
        ),
        "user_interviews" -> passThrough("user_interviews", identity),
        "empathy_map"     -> passThrough("empathy_map", identity)
      )
      val graph = ProjectContextGraph.build[IO](tasks).copy(entryPoint = "human_problem_selection")

      ProjectContextGraph
        .execute(graph, ProjectContextState())
        .compile
        .toList
        .asserting { results =>
          results.map(_._1) should contain("user_interviews")
          results.map(_._1) should not contain "weird_problem_generation"
        }
    }
  }
