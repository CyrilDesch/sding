package chat4s.graph

opaque type WorkflowId = String

object WorkflowId:
  def apply(s: String): WorkflowId             = s
  def random: WorkflowId                       = java.util.UUID.randomUUID().toString
  extension (id: WorkflowId) def value: String = id

final case class StepResult[S](stepId: String, state: S)

/** Handle to a workflow that has been submitted for execution. */
trait WorkflowExecution[F[_], S]:
  def id: WorkflowId

  /** Lazy stream of step results. Consume this in a fiber to drive execution.
    * The stream completes normally when there are no more steps (terminal node reached)
    * or is interrupted when [[cancel]] is called.
    */
  def stream: fs2.Stream[F, StepResult[S]]

  /** Interrupt execution. The stream will terminate cleanly after the current step completes. */
  def cancel: F[Unit]
