package sding.workflow

import cats.effect.Async
import java.util as ju
import org.bsc.langgraph4j.CompiledGraph
import org.bsc.langgraph4j.StateGraph
import org.bsc.langgraph4j.state.AgentState
import scala.jdk.CollectionConverters.*

private[workflow] final class LiveCompiledGraphF[F[_]: Async, S <: AgentState](
    compiled: CompiledGraph[S]
) extends CompiledGraphF[F, S]:

  def stream(inputs: Map[String, Any]): fs2.Stream[F, NodeOutputF[S]] =
    fs2.Stream.evalSeq(
      Async[F].blocking {
        val javaInputs = toJavaMap(inputs)
        val generator  = compiled.stream(javaInputs)
        val results    = scala.collection.mutable.ListBuffer.empty[NodeOutputF[S]]
        val iter       = generator.iterator()
        while iter.hasNext do
          val nodeOutput = iter.next()
          val nodeId     = nodeOutput.node()
          if nodeId != StateGraph.START && nodeId != StateGraph.END then
            results += NodeOutputF(nodeId, nodeOutput.state())
        results.toList
      }
    )

  def execute(inputs: Map[String, Any]): F[Map[String, Any]] =
    Async[F].blocking {
      val javaInputs = toJavaMap(inputs)
      val optState   = compiled.invoke(javaInputs)
      if optState.isPresent then optState.get().data().asScala.toMap
      else Map.empty[String, Any]
    }

  private def toJavaMap(m: Map[String, Any]): ju.Map[String, Object] =
    val javaMap = new ju.HashMap[String, Object]()
    m.foreach { case (k, v) => javaMap.put(k, v.asInstanceOf[Object]) }
    javaMap
