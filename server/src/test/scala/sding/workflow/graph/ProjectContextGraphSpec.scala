package sding.workflow.graph

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import chat4s.graph.WorkflowEngine
import chat4s.graph.WorkflowId
import chat4s.graph.WorkflowJournal
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.typelevel.otel4s.trace.Tracer
import sding.protocol.WorkflowStep
import sding.workflow.state.ProjectContextState
import sding.workflow.TaskNode

class ProjectContextGraphSpec extends AsyncWordSpec with AsyncIOSpec with Matchers:

  given Tracer[IO] = Tracer.noop

  private val engine: WorkflowEngine[IO] = WorkflowEngine.make[IO]

  private def passThrough(step: WorkflowStep, mutation: ProjectContextState => ProjectContextState): TaskNode[IO] =
    new TaskNode[IO]:
      val name                                                         = step
      def execute(state: ProjectContextState): IO[ProjectContextState] =
        IO.pure(mutation(state))

  "ProjectContextGraph.build" should {

    "construct a graph with all 19 expected nodes when provided" in {
      val tasks = WorkflowStep.values.map(s => s -> passThrough(s, identity)).toMap[WorkflowStep, TaskNode[IO]]
      val graph = ProjectContextGraph.build[IO](tasks)

      graph.entryPoint shouldBe WorkflowStep.HumanRequirements.snakeName
      graph.edges should have length 14
      graph.conditionalEdges should have length 4
      graph.nodes should have size 19
    }

    "set correct conditional edge routing from human_problem_selection" in {
      val tasks = Map(WorkflowStep.HumanProblemSelection -> passThrough(WorkflowStep.HumanProblemSelection, identity))
      val graph = ProjectContextGraph.build[IO](tasks)
      val ce    = graph.conditionalEdges.find(_.from == WorkflowStep.HumanProblemSelection.snakeName)
      ce shouldBe defined

      val stateNoFeedback = ProjectContextState()
      ce.get.router(stateNoFeedback) shouldBe "proceed"
      ce.get.targets("proceed") shouldBe WorkflowStep.UserInterviews.snakeName

      val stateWithFeedback = stateNoFeedback.appendFeedback("desired_modification", "redo")
      ce.get.router(stateWithFeedback) shouldBe "loop_back"
      ce.get.targets("loop_back") shouldBe WorkflowStep.WeirdProblemGeneration.snakeName
    }

    "set correct conditional edge routing from human_jtbd_selection" in {
      val tasks = Map(WorkflowStep.HumanJtbdSelection -> passThrough(WorkflowStep.HumanJtbdSelection, identity))
      val graph = ProjectContextGraph.build[IO](tasks)
      val ce    = graph.conditionalEdges.find(_.from == WorkflowStep.HumanJtbdSelection.snakeName)
      ce shouldBe defined

      ce.get.router(ProjectContextState()) shouldBe "proceed"
      ce.get.targets("proceed") shouldBe WorkflowStep.Hmw.snakeName

      val withFeedback = ProjectContextState().appendFeedback("jtbd_improvement_feedback", "improve")
      ce.get.router(withFeedback) shouldBe "loop_back"
      ce.get.targets("loop_back") shouldBe WorkflowStep.UserInterviews.snakeName
    }

    "set correct conditional edge routing from human_comprehensive_selection" in {
      val tasks =
        Map(WorkflowStep.HumanComprehensiveSelection -> passThrough(WorkflowStep.HumanComprehensiveSelection, identity))
      val graph = ProjectContextGraph.build[IO](tasks)
      val ce    = graph.conditionalEdges.find(_.from == WorkflowStep.HumanComprehensiveSelection.snakeName)
      ce shouldBe defined

      ce.get.router(ProjectContextState()) shouldBe "proceed"
      ce.get.targets("proceed") shouldBe WorkflowStep.PrototypeBuilds.snakeName

      val withFeedback = ProjectContextState().appendFeedback("comprehensive_analysis_feedback", "refine")
      ce.get.router(withFeedback) shouldBe "loop_back"
      ce.get.targets("loop_back") shouldBe WorkflowStep.Hmw.snakeName
    }

    "set correct conditional edge routing from human_project_selection" in {
      val tasks =
        Map(WorkflowStep.HumanProjectSelection -> passThrough(WorkflowStep.HumanProjectSelection, identity))
      val graph = ProjectContextGraph.build[IO](tasks)
      val ce    = graph.conditionalEdges.find(_.from == WorkflowStep.HumanProjectSelection.snakeName)
      ce shouldBe defined

      ce.get.router(ProjectContextState()) shouldBe "proceed"
      ce.get.targets("proceed") shouldBe WorkflowStep.PremiumReport.snakeName

      val withFeedback = ProjectContextState().appendFeedback("human_project_revision_feedback", "redo")
      ce.get.router(withFeedback) shouldBe "loop_back"
      ce.get.targets("loop_back") shouldBe WorkflowStep.PrototypeBuilds.snakeName
    }
  }

  "ProjectContextGraph execution via WorkflowEngine" should {

    "execute a simple linear subpath" in {
      val tasks = Map(
        WorkflowStep.HumanRequirements -> passThrough(WorkflowStep.HumanRequirements, _.copy(workflowName = "step1")),
        WorkflowStep.WeirdProblemGeneration -> passThrough(
          WorkflowStep.WeirdProblemGeneration,
          _.copy(projectLanguage = Some("English"))
        )
      )
      val graph = ProjectContextGraph.build[IO](tasks)
      for
        journal   <- WorkflowJournal.inMemory[IO, ProjectContextState]
        execution <- engine.start(WorkflowId.random, graph, ProjectContextState(), journal)
        results   <- execution.stream.compile.toList
      yield
        results should have length 2
        results.head.stepId shouldBe WorkflowStep.HumanRequirements.snakeName
        results.head.state.workflowName shouldBe "step1"
        results(1).stepId shouldBe WorkflowStep.WeirdProblemGeneration.snakeName
        results(1).state.projectLanguage shouldBe Some("English")
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
      val graph = ProjectContextGraph.build[IO](tasks).copy(entryPoint = WorkflowStep.HumanProblemSelection.snakeName)
      for
        journal   <- WorkflowJournal.inMemory[IO, ProjectContextState]
        execution <- engine.start(WorkflowId.random, graph, ProjectContextState(), journal)
        results   <- execution.stream.compile.toList
      yield results.map(_.stepId) should contain(WorkflowStep.WeirdProblemGeneration.snakeName)
    }

    "follow proceed path when no feedback present" in {
      val tasks = Map(
        WorkflowStep.HumanProblemSelection -> passThrough(WorkflowStep.HumanProblemSelection, identity),
        WorkflowStep.UserInterviews        -> passThrough(WorkflowStep.UserInterviews, identity),
        WorkflowStep.EmpathyMap            -> passThrough(WorkflowStep.EmpathyMap, identity)
      )
      val graph = ProjectContextGraph.build[IO](tasks).copy(entryPoint = WorkflowStep.HumanProblemSelection.snakeName)
      for
        journal   <- WorkflowJournal.inMemory[IO, ProjectContextState]
        execution <- engine.start(WorkflowId.random, graph, ProjectContextState(), journal)
        results   <- execution.stream.compile.toList
      yield
        results.map(_.stepId) should contain(WorkflowStep.UserInterviews.snakeName)
        results.map(_.stepId) should not contain WorkflowStep.WeirdProblemGeneration.snakeName
    }
  }
