package sding.service

import cats.effect.Async
import cats.syntax.all.*
import chat4s.graph.WorkflowCheckpoint
import chat4s.graph.WorkflowId
import chat4s.graph.WorkflowJournal
import io.circe.parser.decode
import io.circe.syntax.*
import sding.domain.StepId
import sding.repository.StepRepository
import sding.workflow.state.ProjectContextState

/** Workflow journal backed by [[StepRepository]].
  * Persists checkpoint state and the last completed step name to the `steps` table.
  */
final class StepRepoWorkflowJournal[F[_]: Async](
    stepRepo: StepRepository[F],
    stepId: StepId
) extends WorkflowJournal[F, ProjectContextState]:

  def save(workflowId: WorkflowId, checkpoint: WorkflowCheckpoint[ProjectContextState]): F[Unit] =
    stepRepo.updateState(stepId, checkpoint.state.asJson.noSpaces, checkpoint.completedStep)

  def load(workflowId: WorkflowId): F[Option[WorkflowCheckpoint[ProjectContextState]]] =
    stepRepo.findById(stepId).flatMap {
      case None       => Async[F].pure(None)
      case Some(step) =>
        Async[F].fromEither(
          decode[ProjectContextState](step.jsonState)
            .map(state => Some(WorkflowCheckpoint(step.currentTask, state)))
            .left
            .map(e => new RuntimeException(s"Failed to parse workflow state: ${e.getMessage}"))
        )
    }
