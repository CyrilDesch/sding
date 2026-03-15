package sding.repository

import cats.effect.Sync
import io.getquill.*
import java.time.Instant
import java.util.UUID
import sding.domain.ChatId
import sding.domain.ProjectId
import sding.domain.StepId

final case class ChatRecord(
    id: ChatId,
    projectId: ProjectId,
    currentStepId: Option[StepId],
    title: String,
    createdAt: Instant
)

final class ChatRepository[F[_]: Sync](ctx: PostgresJdbcContext[SnakeCase]):
  import ctx.*

  private given MappedEncoding[ChatId, UUID]    = MappedEncoding(_.value)
  private given MappedEncoding[UUID, ChatId]    = MappedEncoding(ChatId.apply)
  private given MappedEncoding[ProjectId, UUID] = MappedEncoding(_.value)
  private given MappedEncoding[UUID, ProjectId] = MappedEncoding(ProjectId.apply)
  private given MappedEncoding[StepId, UUID]    = MappedEncoding(_.value)
  private given MappedEncoding[UUID, StepId]    = MappedEncoding(StepId.apply)

  inline given SchemaMeta[ChatRecord] = schemaMeta("chats")

  def findById(id: ChatId): F[Option[ChatRecord]] = Sync[F].blocking {
    run(query[ChatRecord].filter(_.id == lift(id))).headOption
  }

  def findByProject(pid: ProjectId): F[List[ChatRecord]] = Sync[F].blocking {
    run(query[ChatRecord].filter(_.projectId == lift(pid)))
  }

  def create(pid: ProjectId, sid: StepId, title: String): F[ChatRecord] = Sync[F].blocking {
    val record = ChatRecord(
      id = ChatId.random,
      projectId = pid,
      currentStepId = Some(sid),
      title = title,
      createdAt = Instant.now()
    )
    run(query[ChatRecord].insertValue(lift(record)))
    record
  }

object ChatRepository:
  def make[F[_]: Sync](ctx: PostgresJdbcContext[SnakeCase]): ChatRepository[F] =
    new ChatRepository(ctx)
