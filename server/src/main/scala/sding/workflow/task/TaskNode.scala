package sding.workflow.task

import sding.protocol.WorkflowStep
import sding.workflow.state.ProjectContextState

trait TaskNode[F[_]]:
  def name: WorkflowStep
  def execute(state: ProjectContextState): F[ProjectContextState]
