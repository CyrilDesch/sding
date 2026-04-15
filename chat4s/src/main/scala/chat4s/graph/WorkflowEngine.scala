package chat4s.graph

import cats.effect.Async
import cats.effect.Ref
import cats.syntax.all.*
import fs2.concurrent.SignallingRef
import org.typelevel.otel4s.trace.Tracer

/** Manages the lifecycle of workflow executions: start, stop, and resume after crash. */
trait WorkflowEngine[F[_]]:

  /** Start a brand-new workflow from [[initialState]] at [[wf.entryPoint]].
    * Checkpoints are persisted to [[journal]] after each step completes.
    */
  def start[S](
      workflowId: WorkflowId,
      wf: WorkflowDef[F, S],
      initialState: S,
      journal: WorkflowJournal[F, S]
  ): F[WorkflowExecution[F, S]]

  /** Resume a workflow from its last persisted checkpoint.
    * Returns [[None]] if no checkpoint is found for [[workflowId]].
    */
  def resume[S](
      workflowId: WorkflowId,
      wf: WorkflowDef[F, S],
      journal: WorkflowJournal[F, S]
  ): F[Option[WorkflowExecution[F, S]]]

object WorkflowEngine:
  def make[F[_]: Async: Tracer]: WorkflowEngine[F] = new LiveWorkflowEngine[F]

private final class LiveWorkflowEngine[F[_]: Async: Tracer] extends WorkflowEngine[F]:

  def start[S](
      workflowId: WorkflowId,
      wf: WorkflowDef[F, S],
      initialState: S,
      journal: WorkflowJournal[F, S]
  ): F[WorkflowExecution[F, S]] =
    startFrom(workflowId, wf, initialState, wf.entryPoint, journal)

  def resume[S](
      workflowId: WorkflowId,
      wf: WorkflowDef[F, S],
      journal: WorkflowJournal[F, S]
  ): F[Option[WorkflowExecution[F, S]]] =
    journal.load(workflowId).flatMap {
      case None                                                => Async[F].pure(None)
      case Some(WorkflowCheckpoint(completedStep, savedState)) =>
        val nextStep = completedStep
          .flatMap(step => wf.resolveNext(step, savedState))
          .getOrElse(wf.entryPoint)
        startFrom(workflowId, wf, savedState, nextStep, journal).map(Some(_))
    }

  private def startFrom[S](
      workflowId: WorkflowId,
      wf: WorkflowDef[F, S],
      initialState: S,
      startStep: String,
      journal: WorkflowJournal[F, S]
  ): F[WorkflowExecution[F, S]] =
    SignallingRef.of[F, Boolean](false).map { cancelSignal =>
      // Guard at stream entry: if cancel() was already called, emit nothing.
      val executionStream = fs2.Stream.eval(cancelSignal.get).flatMap { cancelled =>
        if cancelled then fs2.Stream.empty
        else buildStream(workflowId, wf, initialState, startStep, journal, cancelSignal)
      }
      new LiveWorkflowExecution[F, S](workflowId, executionStream, cancelSignal)
    }

  private def buildStream[S](
      workflowId: WorkflowId,
      wf: WorkflowDef[F, S],
      initialState: S,
      startStep: String,
      journal: WorkflowJournal[F, S],
      cancelSignal: SignallingRef[F, Boolean]
  ): fs2.Stream[F, StepResult[S]] =
    fs2.Stream.eval(Ref.of[F, S](initialState)).flatMap { stateRef =>
      def step(nodeId: String): fs2.Stream[F, StepResult[S]] =
        wf.nodes.get(nodeId) match
          case None       => fs2.Stream.empty
          case Some(node) =>
            fs2.Stream
              .eval(
                stateRef.get
                  .flatMap(s => Tracer[F].spanBuilder(nodeId).build.surround(node.execute(s)))
                  .flatTap(stateRef.set)
                  .flatTap(s => journal.save(workflowId, WorkflowCheckpoint(Some(nodeId), s)))
              )
              .map(s => StepResult(nodeId, s))
              .flatMap(r =>
                fs2.Stream.emit(r) ++ wf
                  .resolveNext(r.stepId, r.state)
                  .fold(
                    fs2.Stream.empty.covaryAll[F, StepResult[S]]
                  )(step)
              )

      step(startStep).interruptWhen(cancelSignal)
    }

private final class LiveWorkflowExecution[F[_], S](
    val id: WorkflowId,
    private val executionStream: fs2.Stream[F, StepResult[S]],
    private val cancelSignal: SignallingRef[F, Boolean]
) extends WorkflowExecution[F, S]:
  def stream: fs2.Stream[F, StepResult[S]] = executionStream
  def cancel: F[Unit]                      = cancelSignal.set(true)
