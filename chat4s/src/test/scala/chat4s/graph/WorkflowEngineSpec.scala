package chat4s.graph

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.typelevel.otel4s.trace.Tracer

class WorkflowEngineSpec extends AsyncWordSpec with AsyncIOSpec with Matchers:

  given Tracer[IO] = Tracer.noop

  private val engine: WorkflowEngine[IO] = WorkflowEngine.make[IO]

  private def step(nodeId: String, f: Int => Int = identity): Step[IO, Int] =
    new Step[IO, Int]:
      def id                           = nodeId
      def execute(state: Int): IO[Int] = IO.pure(f(state))

  "WorkflowEngine.start" should {

    "execute a linear graph and emit one StepResult per node" in {
      val wf = WorkflowDef[IO, Int](
        nodes = Map("a" -> step("a", _ + 1), "b" -> step("b", _ + 10), "c" -> step("c", _ + 100)),
        edges = List(WorkflowEdge("a", "b"), WorkflowEdge("b", "c")),
        conditionalEdges = Nil,
        entryPoint = "a"
      )
      for
        journal   <- WorkflowJournal.inMemory[IO, Int]
        execution <- engine.start(WorkflowId("wf-1"), wf, 0, journal)
        results   <- execution.stream.compile.toList
      yield
        results.map(_.stepId) shouldBe List("a", "b", "c")
        results.map(_.state) shouldBe List(1, 11, 111)
    }

    "follow the correct branch in a conditional edge" in {
      val wf = WorkflowDef[IO, Int](
        nodes = Map(
          "gate"  -> step("gate"),
          "left"  -> step("left", _ + 1),
          "right" -> step("right", _ + 10)
        ),
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
      for
        journal  <- WorkflowJournal.inMemory[IO, Int]
        execLow  <- engine.start(WorkflowId("wf-low"), wf, 3, journal)
        execHigh <- engine.start(WorkflowId("wf-high"), wf, 7, journal)
        low      <- execLow.stream.compile.toList
        high     <- execHigh.stream.compile.toList
      yield
        low.map(_.stepId) shouldBe List("gate", "right")
        high.map(_.stepId) shouldBe List("gate", "left")
    }

    "save a checkpoint to the journal after each step" in {
      val wf = WorkflowDef[IO, Int](
        nodes = Map("a" -> step("a", _ + 1), "b" -> step("b", _ + 10)),
        edges = List(WorkflowEdge("a", "b")),
        conditionalEdges = Nil,
        entryPoint = "a"
      )
      val wfId = WorkflowId("wf-journal")
      for
        journal   <- WorkflowJournal.inMemory[IO, Int]
        execution <- engine.start(wfId, wf, 0, journal)
        _         <- execution.stream.compile.drain
        cp        <- journal.load(wfId)
      yield cp shouldBe Some(WorkflowCheckpoint(Some("b"), 11))
    }
  }

  "WorkflowEngine.resume" should {

    "return None when no checkpoint exists" in {
      val wf = WorkflowDef[IO, Int](
        nodes = Map("a" -> step("a")),
        edges = Nil,
        conditionalEdges = Nil,
        entryPoint = "a"
      )
      for
        journal <- WorkflowJournal.inMemory[IO, Int]
        result  <- engine.resume(WorkflowId("unknown"), wf, journal)
      yield result shouldBe None
    }

    "resume from the step after the last completed step" in {
      val wf = WorkflowDef[IO, Int](
        nodes = Map("a" -> step("a", _ + 1), "b" -> step("b", _ + 10), "c" -> step("c", _ + 100)),
        edges = List(WorkflowEdge("a", "b"), WorkflowEdge("b", "c")),
        conditionalEdges = Nil,
        entryPoint = "a"
      )
      val wfId = WorkflowId("wf-resume")
      // Simulate crash after "a" completed with state = 1
      for
        journal <- WorkflowJournal.inMemory[IO, Int]
        _       <- journal.save(wfId, WorkflowCheckpoint(Some("a"), 1))
        execOpt <- engine.resume(wfId, wf, journal)
        results <- execOpt.get.stream.compile.toList
      yield
        // Should start from "b", not "a"
        results.map(_.stepId) shouldBe List("b", "c")
        results.head.state shouldBe 11
    }
  }

  "WorkflowExecution.cancel" should {

    "stop the stream before all steps complete" in {
      var executed                                    = List.empty[String]
      def trackingStep(nodeId: String): Step[IO, Int] =
        new Step[IO, Int]:
          def id                           = nodeId
          def execute(state: Int): IO[Int] =
            IO { executed = executed :+ nodeId }.as(state)

      val wf = WorkflowDef[IO, Int](
        nodes = Map(
          "a" -> trackingStep("a"),
          "b" -> trackingStep("b"),
          "c" -> trackingStep("c")
        ),
        edges = List(WorkflowEdge("a", "b"), WorkflowEdge("b", "c")),
        conditionalEdges = Nil,
        entryPoint = "a"
      )
      for
        journal   <- WorkflowJournal.inMemory[IO, Int]
        execution <- engine.start(WorkflowId("wf-cancel"), wf, 0, journal)
        // Take only the first step result, then cancel
        _ <- execution.stream.take(1).compile.drain
        _ <- execution.cancel
        // After cancel the stream should not emit more
        remaining <- execution.stream.compile.toList
      yield remaining shouldBe empty
    }
  }
