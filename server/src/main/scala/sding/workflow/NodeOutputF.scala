package sding.workflow

import org.bsc.langgraph4j.state.AgentState

final case class NodeOutputF[S <: AgentState](nodeId: String, state: S)
