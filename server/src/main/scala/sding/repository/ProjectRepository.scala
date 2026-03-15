package sding.repository

import cats.effect.Sync
import io.getquill.*
import java.time.Instant
import java.util.UUID
import sding.domain.ProjectId
import sding.domain.UserId

final case class ProjectRecord(
    id: ProjectId,
    userId: UserId,
    name: String,
    status: ProjectStatus,
    language: String,
    deletedAt: Option[Instant] = None
)

final class ProjectRepository[F[_]: Sync](ctx: PostgresJdbcContext[SnakeCase]):
  import ctx.*

  private given MappedEncoding[ProjectId, UUID] = MappedEncoding(_.value)
  private given MappedEncoding[UUID, ProjectId] = MappedEncoding(ProjectId.apply)
  private given MappedEncoding[UserId, UUID]    = MappedEncoding(_.value)
  private given MappedEncoding[UUID, UserId]    = MappedEncoding(UserId.apply)

  private given MappedEncoding[ProjectStatus, String] = MappedEncoding {
    case ProjectStatus.Draft      => "draft"
    case ProjectStatus.InProgress => "in_progress"
    case ProjectStatus.Completed  => "completed"
    case ProjectStatus.Archived   => "archived"
  }
  private given MappedEncoding[String, ProjectStatus] = MappedEncoding {
    case "draft"       => ProjectStatus.Draft
    case "in_progress" => ProjectStatus.InProgress
    case "completed"   => ProjectStatus.Completed
    case "archived"    => ProjectStatus.Archived
    case s             => throw new IllegalArgumentException(s"Unknown project_status: $s")
  }

  inline given SchemaMeta[ProjectRecord] = schemaMeta("projects")

  def findById(id: ProjectId): F[Option[ProjectRecord]] = Sync[F].blocking {
    run(query[ProjectRecord].filter(_.id == lift(id))).headOption
  }

  def findByUser(uid: UserId): F[List[ProjectRecord]] = Sync[F].blocking {
    run(query[ProjectRecord].filter(r => r.userId == lift(uid) && r.deletedAt.isEmpty))
  }

  def softDelete(id: ProjectId): F[Unit] = Sync[F].blocking {
    run(
      query[ProjectRecord]
        .filter(_.id == lift(id))
        .update(_.deletedAt -> lift(Some(Instant.now()): Option[Instant]))
    )
    ()
  }

  def create(uid: UserId, name: String, language: String): F[ProjectRecord] = Sync[F].blocking {
    val record = ProjectRecord(
      id = ProjectId.random,
      userId = uid,
      name = name,
      status = ProjectStatus.InProgress,
      language = language
    )
    run(query[ProjectRecord].insertValue(lift(record)))
    record
  }

  def updateStatus(id: ProjectId, status: ProjectStatus): F[Unit] = Sync[F].blocking {
    run(query[ProjectRecord].filter(_.id == lift(id)).update(_.status -> lift(status)))
    ()
  }

object ProjectRepository:
  def make[F[_]: Sync](ctx: PostgresJdbcContext[SnakeCase]): ProjectRepository[F] =
    new ProjectRepository(ctx)
