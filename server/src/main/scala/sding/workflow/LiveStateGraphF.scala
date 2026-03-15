package sding.workflow

import cats.effect.Async
import cats.effect.std.Dispatcher
import cats.syntax.all.*
import java.util as ju
import org.bsc.langgraph4j.StateGraph
import org.bsc.langgraph4j.action.AsyncEdgeAction
import org.bsc.langgraph4j.action.AsyncNodeAction
import org.bsc.langgraph4j.state.AgentState
import scala.jdk.CollectionConverters.*

object LiveStateGraphF:
  def make[F[_]: Async, S <: AgentState](
      stateFactory: ju.Map[String, Object] => S
  ): F[StateGraphF[F, S]] =
    Async[F].pure(
      new LiveStateGraphFImpl[F, S](
        stateFactory,
        nodes = List.empty,
        edges = List.empty,
        conditionalEdges = List.empty
      )
    )

private final class LiveStateGraphFImpl[F[_]: Async, S <: AgentState](
    stateFactory: ju.Map[String, Object] => S,
    nodes: List[(String, S => F[Map[String, Any]])],
    edges: List[(String, String)],
    conditionalEdges: List[(String, S => F[String], Map[String, String])]
) extends StateGraphF[F, S]:

  def addNode(name: String, action: S => F[Map[String, Any]]): StateGraphF[F, S] =
    new LiveStateGraphFImpl(stateFactory, (name, action) :: nodes, edges, conditionalEdges)

  def addEdge(from: String, to: String): StateGraphF[F, S] =
    new LiveStateGraphFImpl(stateFactory, nodes, (from, to) :: edges, conditionalEdges)

  def addConditionalEdge(
      from: String,
      router: S => F[String],
      mapping: Map[String, String]
  ): StateGraphF[F, S] =
    new LiveStateGraphFImpl(stateFactory, nodes, edges, (from, router, mapping) :: conditionalEdges)

  def compile: F[CompiledGraphF[F, S]] =
    Dispatcher.parallel[F](await = true).allocated.flatMap { case (dispatcher, _) =>
      Async[F].delay {
        val graph = new StateGraph[S](stateFactory(_))
        nodes.reverse.foreach { case (name, action) =>
          graph.addNode(name, liftNodeAction(dispatcher, action))
        }
        edges.reverse.foreach { case (from, to) =>
          graph.addEdge(from, to)
        }
        conditionalEdges.reverse.foreach { case (from, router, mapping) =>
          graph.addConditionalEdges(from, liftEdgeAction(dispatcher, router), mapping.asJava)
        }
        val compiled = graph.compile()
        new LiveCompiledGraphF[F, S](compiled): CompiledGraphF[F, S]
      }
    }

  private def liftNodeAction(dispatcher: Dispatcher[F], action: S => F[Map[String, Any]]): AsyncNodeAction[S] =
    (state: S) =>
      dispatcher.unsafeToCompletableFuture(
        Async[F].map(action(state)) { scalaMap =>
          val javaMap = new ju.HashMap[String, Object]()
          scalaMap.foreach { case (k, v) => javaMap.put(k, v.asInstanceOf[Object]) }
          javaMap: ju.Map[String, Object]
        }
      )

  private def liftEdgeAction(dispatcher: Dispatcher[F], router: S => F[String]): AsyncEdgeAction[S] =
    (state: S) => dispatcher.unsafeToCompletableFuture(router(state))
