package sding.workflow

import org.bsc.langgraph4j.state.AgentState

trait CompiledGraphF[F[_], S <: AgentState]:
  def stream(inputs: Map[String, Any]): fs2.Stream[F, NodeOutputF[S]]
  def execute(inputs: Map[String, Any]): F[Map[String, Any]]
