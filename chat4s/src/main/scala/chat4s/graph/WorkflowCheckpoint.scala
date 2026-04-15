package chat4s.graph

/** Snapshot of a workflow at a point in time.
  *
  * @param completedStep
  *   The ID of the last successfully completed step, or [[None]] if the workflow has not yet started.
  * @param state
  *   The state produced by that step (or the initial state if not started).
  */
final case class WorkflowCheckpoint[S](completedStep: Option[String], state: S)
