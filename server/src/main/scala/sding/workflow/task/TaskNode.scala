package sding.workflow.task

import sding.workflow.state.ProjectContextState

trait TaskNode[F[_]]:
  def name: String
  def execute(state: ProjectContextState): F[ProjectContextState]
