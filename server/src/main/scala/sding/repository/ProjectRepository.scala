package sding.repository

import cats.effect.Sync
import io.getquill.*
import java.util.UUID
import sding.domain.ProjectId
import sding.domain.UserId

final case class ProjectRecord(
    id: ProjectId,
    userId: UserId,
    name: String,
    status: String,
    language: String
)

final class ProjectRepository[F[_]: Sync](ctx: PostgresJdbcContext[SnakeCase]):
  import ctx.*

  private given MappedEncoding[ProjectId, UUID] = MappedEncoding(_.value)
  private given MappedEncoding[UUID, ProjectId] = MappedEncoding(ProjectId.apply)
  private given MappedEncoding[UserId, UUID]    = MappedEncoding(_.value)
  private given MappedEncoding[UUID, UserId]    = MappedEncoding(UserId.apply)

  inline given SchemaMeta[ProjectRecord] = schemaMeta("projects")

  def findById(id: ProjectId): F[Option[ProjectRecord]] = Sync[F].blocking {
    run(query[ProjectRecord].filter(_.id == lift(id))).headOption
  }

  def findByUser(uid: UserId): F[List[ProjectRecord]] = Sync[F].blocking {
    run(query[ProjectRecord].filter(_.userId == lift(uid)))
  }

  def create(uid: UserId, name: String, language: String): F[ProjectRecord] = Sync[F].blocking {
    val record = ProjectRecord(
      id = ProjectId.random,
      userId = uid,
      name = name,
      status = "active",
      language = language
    )
    run(query[ProjectRecord].insertValue(lift(record)))
    record
  }

  def updateStatus(id: ProjectId, status: String): F[Unit] = Sync[F].blocking {
    run(query[ProjectRecord].filter(_.id == lift(id)).update(_.status -> lift(status)))
    ()
  }

object ProjectRepository:
  def make[F[_]: Sync](ctx: PostgresJdbcContext[SnakeCase]): ProjectRepository[F] =
    new ProjectRepository(ctx)
