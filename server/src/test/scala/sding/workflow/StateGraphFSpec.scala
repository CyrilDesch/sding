package sding.workflow

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import org.bsc.langgraph4j.GraphDefinition
import org.bsc.langgraph4j.state.AgentState
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

class StateGraphFSpec extends AsyncWordSpec with AsyncIOSpec with Matchers:

  "StateGraphF" should {

    "build a simple linear graph and execute it" in {
      for
        graph    <- LiveStateGraphF.make[IO, AgentState](m => new AgentState(m))
        compiled <- graph
          .addNode("greet", _ => IO.pure(Map("greeting" -> ("hello": Any))))
          .addEdge(GraphDefinition.START, "greet")
          .addEdge("greet", GraphDefinition.END)
          .compile
        result <- compiled.execute(Map.empty)
      yield result("greeting") shouldBe "hello"
    }

    "support conditional edges" in {
      for
        graph    <- LiveStateGraphF.make[IO, AgentState](m => new AgentState(m))
        compiled <- graph
          .addNode("check", _ => IO.pure(Map("route" -> ("b": Any))))
          .addNode("pathA", _ => IO.pure(Map("result" -> ("A": Any))))
          .addNode("pathB", _ => IO.pure(Map("result" -> ("B": Any))))
          .addEdge(GraphDefinition.START, "check")
          .addConditionalEdge(
            "check",
            s => IO.pure(s.data().getOrDefault("route", "a").toString),
            Map("a" -> "pathA", "b" -> "pathB")
          )
          .addEdge("pathA", GraphDefinition.END)
          .addEdge("pathB", GraphDefinition.END)
          .compile
        result <- compiled.execute(Map.empty)
      yield result("result") shouldBe "B"
    }

    "stream node outputs as fs2.Stream" in {
      for
        graph    <- LiveStateGraphF.make[IO, AgentState](m => new AgentState(m))
        compiled <- graph
          .addNode("step1", _ => IO.pure(Map("v" -> ("1": Any))))
          .addNode("step2", _ => IO.pure(Map("v" -> ("2": Any))))
          .addEdge(GraphDefinition.START, "step1")
          .addEdge("step1", "step2")
          .addEdge("step2", GraphDefinition.END)
          .compile
        outputs <- compiled.stream(Map.empty).compile.toList
      yield {
        outputs should have size 2
        outputs.map(_.nodeId) shouldBe List("step1", "step2")
      }
    }

    "propagate node errors into F" in {
      for
        graph    <- LiveStateGraphF.make[IO, AgentState](m => new AgentState(m))
        compiled <- graph
          .addNode("boom", _ => IO.raiseError(new RuntimeException("kaboom")))
          .addEdge(GraphDefinition.START, "boom")
          .addEdge("boom", GraphDefinition.END)
          .compile
        result <- compiled.execute(Map.empty).attempt
      yield result.isLeft shouldBe true
    }
  }
