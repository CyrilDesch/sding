package sding.repository

import cats.effect.Sync
import io.getquill.*
import java.util.UUID
import sding.domain.ProjectId
import sding.domain.StepId

final case class StepRecord(
    id: StepId,
    projectId: ProjectId,
    stepType: String,
    jsonState: String,
    isFinished: Boolean,
    currentTask: Option[String]
)

final class StepRepository[F[_]: Sync](ctx: PostgresJdbcContext[SnakeCase]):
  import ctx.*

  private given MappedEncoding[StepId, UUID]    = MappedEncoding(_.value)
  private given MappedEncoding[UUID, StepId]    = MappedEncoding(StepId.apply)
  private given MappedEncoding[ProjectId, UUID] = MappedEncoding(_.value)
  private given MappedEncoding[UUID, ProjectId] = MappedEncoding(ProjectId.apply)

  inline given SchemaMeta[StepRecord] = schemaMeta("steps")

  def findById(id: StepId): F[Option[StepRecord]] = Sync[F].blocking {
    run(query[StepRecord].filter(_.id == lift(id))).headOption
  }

  def create(pid: ProjectId, stepType: String): F[StepRecord] = Sync[F].blocking {
    val record = StepRecord(
      id = StepId.random,
      projectId = pid,
      stepType = stepType,
      jsonState = "{}",
      isFinished = false,
      currentTask = None
    )
    run(query[StepRecord].insertValue(lift(record)))
    record
  }

  def updateState(id: StepId, jsonState: String, currentTask: Option[String]): F[Unit] = Sync[F].blocking {
    run(
      query[StepRecord]
        .filter(_.id == lift(id))
        .update(
          _.jsonState   -> lift(jsonState),
          _.currentTask -> lift(currentTask)
        )
    )
    ()
  }

  def markFinished(id: StepId): F[Unit] = Sync[F].blocking {
    run(query[StepRecord].filter(_.id == lift(id)).update(_.isFinished -> lift(true)))
    ()
  }

object StepRepository:
  def make[F[_]: Sync](ctx: PostgresJdbcContext[SnakeCase]): StepRepository[F] =
    new StepRepository(ctx)
