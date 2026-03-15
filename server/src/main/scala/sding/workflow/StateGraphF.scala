package sding.workflow

import org.bsc.langgraph4j.state.AgentState

trait StateGraphF[F[_], S <: AgentState]:
  def addNode(name: String, action: S => F[Map[String, Any]]): StateGraphF[F, S]
  def addEdge(from: String, to: String): StateGraphF[F, S]
  def addConditionalEdge(
      from: String,
      router: S => F[String],
      mapping: Map[String, String]
  ): StateGraphF[F, S]
  def compile: F[CompiledGraphF[F, S]]
