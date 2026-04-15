package sding.workflow

import chat4s.graph.Step
import sding.protocol.WorkflowStep
import sding.workflow.state.ProjectContextState

trait TaskNode[F[_]] extends Step[F, ProjectContextState]:
  def name: WorkflowStep
  final def id: String = name.snakeName
