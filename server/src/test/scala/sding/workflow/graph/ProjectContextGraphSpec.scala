package sding.workflow.graph

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import sding.protocol.WorkflowStep
import sding.workflow.state.ProjectContextState
import sding.workflow.task.TaskNode

class ProjectContextGraphSpec extends AsyncWordSpec with AsyncIOSpec with Matchers:

  private def passThrough(step: WorkflowStep, mutation: ProjectContextState => ProjectContextState): TaskNode[IO] =
    new TaskNode[IO]:
      val name                                                         = step
      def execute(state: ProjectContextState): IO[ProjectContextState] =
        IO.pure(mutation(state))

  "ProjectContextGraph.build" should {

    "construct a graph with all 19 expected nodes when provided" in {
      val tasks = WorkflowStep.values.map(s => s -> passThrough(s, identity)).toMap[WorkflowStep, TaskNode[IO]]
      val graph = ProjectContextGraph.build[IO](tasks)

      graph.entryPoint shouldBe WorkflowStep.HumanRequirements
      graph.edges should have length 14
      graph.conditionalEdges should have length 4
      graph.nodes should have size 19
    }

    "set correct conditional edge routing from human_problem_selection" in {
      val tasks = Map(WorkflowStep.HumanProblemSelection -> passThrough(WorkflowStep.HumanProblemSelection, identity))
      val graph = ProjectContextGraph.build[IO](tasks)
      val ce    = graph.conditionalEdges.find(_.from == WorkflowStep.HumanProblemSelection)
      ce shouldBe defined

      val stateNoFeedback = ProjectContextState()
      ce.get.router(stateNoFeedback) shouldBe "proceed"
      ce.get.targets("proceed") shouldBe WorkflowStep.UserInterviews

      val stateWithFeedback = stateNoFeedback.appendFeedback("desired_modification", "redo")
      ce.get.router(stateWithFeedback) shouldBe "loop_back"
      ce.get.targets("loop_back") shouldBe WorkflowStep.WeirdProblemGeneration
    }

    "set correct conditional edge routing from human_jtbd_selection" in {
      val tasks = Map(WorkflowStep.HumanJtbdSelection -> passThrough(WorkflowStep.HumanJtbdSelection, identity))
      val graph = ProjectContextGraph.build[IO](tasks)
      val ce    = graph.conditionalEdges.find(_.from == WorkflowStep.HumanJtbdSelection)
      ce shouldBe defined

      ce.get.router(ProjectContextState()) shouldBe "proceed"
      ce.get.targets("proceed") shouldBe WorkflowStep.Hmw

      val withFeedback = ProjectContextState().appendFeedback("jtbd_improvement_feedback", "improve")
      ce.get.router(withFeedback) shouldBe "loop_back"
      ce.get.targets("loop_back") shouldBe WorkflowStep.UserInterviews
    }

    "set correct conditional edge routing from human_comprehensive_selection" in {
      val tasks =
        Map(WorkflowStep.HumanComprehensiveSelection -> passThrough(WorkflowStep.HumanComprehensiveSelection, identity))
      val graph = ProjectContextGraph.build[IO](tasks)
      val ce    = graph.conditionalEdges.find(_.from == WorkflowStep.HumanComprehensiveSelection)
      ce shouldBe defined

      ce.get.router(ProjectContextState()) shouldBe "proceed"
      ce.get.targets("proceed") shouldBe WorkflowStep.PrototypeBuilds

      val withFeedback = ProjectContextState().appendFeedback("comprehensive_analysis_feedback", "refine")
      ce.get.router(withFeedback) shouldBe "loop_back"
      ce.get.targets("loop_back") shouldBe WorkflowStep.Hmw
    }

    "set correct conditional edge routing from human_project_selection" in {
      val tasks =
        Map(WorkflowStep.HumanProjectSelection -> passThrough(WorkflowStep.HumanProjectSelection, identity))
      val graph = ProjectContextGraph.build[IO](tasks)
      val ce    = graph.conditionalEdges.find(_.from == WorkflowStep.HumanProjectSelection)
      ce shouldBe defined

      ce.get.router(ProjectContextState()) shouldBe "proceed"
      ce.get.targets("proceed") shouldBe WorkflowStep.PremiumReport

      val withFeedback = ProjectContextState().appendFeedback("human_project_revision_feedback", "redo")
      ce.get.router(withFeedback) shouldBe "loop_back"
      ce.get.targets("loop_back") shouldBe WorkflowStep.PrototypeBuilds
    }
  }

  "ProjectContextGraph.execute" should {

    "execute a simple linear subpath" in {
      val tasks = Map(
        WorkflowStep.HumanRequirements -> passThrough(WorkflowStep.HumanRequirements, _.copy(workflowName = "step1")),
        WorkflowStep.WeirdProblemGeneration -> passThrough(
          WorkflowStep.WeirdProblemGeneration,
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
          results.head._1 shouldBe WorkflowStep.HumanRequirements
          results.head._2.workflowName shouldBe "step1"
          results(1)._1 shouldBe WorkflowStep.WeirdProblemGeneration
          results(1)._2.projectLanguage shouldBe Some("English")
        }
    }

    "follow conditional edges based on feedback presence" in {
      val tasks = Map(
        WorkflowStep.HumanProblemSelection ->
          passThrough(WorkflowStep.HumanProblemSelection, _.appendFeedback("desired_modification", "retry")),
        WorkflowStep.WeirdProblemGeneration ->
          passThrough(WorkflowStep.WeirdProblemGeneration, _.clearFeedback("desired_modification")),
        WorkflowStep.ProblemReformulation -> passThrough(WorkflowStep.ProblemReformulation, identity),
        WorkflowStep.UserInterviews       -> passThrough(WorkflowStep.UserInterviews, identity)
      )
      val graph = ProjectContextGraph.build[IO](tasks).copy(entryPoint = WorkflowStep.HumanProblemSelection)

      ProjectContextGraph
        .execute(graph, ProjectContextState())
        .compile
        .toList
        .asserting { results =>
          results.map(_._1) should contain(WorkflowStep.WeirdProblemGeneration)
        }
    }

    "follow proceed path when no feedback present" in {
      val tasks = Map(
        WorkflowStep.HumanProblemSelection -> passThrough(WorkflowStep.HumanProblemSelection, identity),
        WorkflowStep.UserInterviews        -> passThrough(WorkflowStep.UserInterviews, identity),
        WorkflowStep.EmpathyMap            -> passThrough(WorkflowStep.EmpathyMap, identity)
      )
      val graph = ProjectContextGraph.build[IO](tasks).copy(entryPoint = WorkflowStep.HumanProblemSelection)

      ProjectContextGraph
        .execute(graph, ProjectContextState())
        .compile
        .toList
        .asserting { results =>
          results.map(_._1) should contain(WorkflowStep.UserInterviews)
          results.map(_._1) should not contain WorkflowStep.WeirdProblemGeneration
        }
    }
  }
