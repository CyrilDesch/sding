package chat4s.graph

import cats.effect.Ref
import cats.effect.Sync
import cats.syntax.all.*

/** Persistence interface for workflow checkpoints. */
trait WorkflowJournal[F[_], S]:
  def save(workflowId: WorkflowId, checkpoint: WorkflowCheckpoint[S]): F[Unit]
  def load(workflowId: WorkflowId): F[Option[WorkflowCheckpoint[S]]]

object WorkflowJournal:

  /** No-op journal: discards checkpoints. Suitable for fire-and-forget workflows. */
  def noop[F[_]: Sync, S]: WorkflowJournal[F, S] =
    new WorkflowJournal[F, S]:
      def save(id: WorkflowId, cp: WorkflowCheckpoint[S]): F[Unit] = Sync[F].unit
      def load(id: WorkflowId): F[Option[WorkflowCheckpoint[S]]]   = Sync[F].pure(None)

  /** In-memory journal backed by a [[Ref]]. Suitable for tests and short-lived workflows. */
  def inMemory[F[_]: Sync, S]: F[WorkflowJournal[F, S]] =
    Ref.of[F, Map[WorkflowId, WorkflowCheckpoint[S]]](Map.empty).map { ref =>
      new WorkflowJournal[F, S]:
        def save(id: WorkflowId, cp: WorkflowCheckpoint[S]): F[Unit] =
          ref.update(_.updated(id, cp))
        def load(id: WorkflowId): F[Option[WorkflowCheckpoint[S]]] =
          ref.get.map(_.get(id))
    }
