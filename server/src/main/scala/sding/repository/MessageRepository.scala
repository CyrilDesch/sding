package sding.repository

import cats.effect.Sync
import io.getquill.*
import java.util.UUID
import sding.domain.ChatId
import sding.domain.MessageId
import sding.domain.UserId

final case class MessageRecord(
    id: MessageId,
    chatId: ChatId,
    senderId: Option[UserId],
    senderType: String,
    content: String,
    contentType: String,
    sourceNode: Option[String]
)

final class MessageRepository[F[_]: Sync](ctx: PostgresJdbcContext[SnakeCase]):
  import ctx.*

  private given MappedEncoding[MessageId, UUID] = MappedEncoding(_.value)
  private given MappedEncoding[UUID, MessageId] = MappedEncoding(MessageId.apply)
  private given MappedEncoding[ChatId, UUID]    = MappedEncoding(_.value)
  private given MappedEncoding[UUID, ChatId]    = MappedEncoding(ChatId.apply)
  private given MappedEncoding[UserId, UUID]    = MappedEncoding(_.value)
  private given MappedEncoding[UUID, UserId]    = MappedEncoding(UserId.apply)

  inline given SchemaMeta[MessageRecord] = schemaMeta("messages")

  def create(
      cid: ChatId,
      senderType: String,
      content: String,
      contentType: String,
      sourceNode: Option[String]
  ): F[MessageRecord] = Sync[F].blocking {
    val record = MessageRecord(
      id = MessageId.random,
      chatId = cid,
      senderId = None,
      senderType = senderType,
      content = content,
      contentType = contentType,
      sourceNode = sourceNode
    )
    run(query[MessageRecord].insertValue(lift(record)))
    record
  }

  def findByChat(cid: ChatId, limit: Int, offset: Int): F[List[MessageRecord]] = Sync[F].blocking {
    run(
      query[MessageRecord]
        .filter(_.chatId == lift(cid))
        .sortBy(_.id)(Ord.asc)
        .drop(lift(offset))
        .take(lift(limit))
    )
  }

object MessageRepository:
  def make[F[_]: Sync](ctx: PostgresJdbcContext[SnakeCase]): MessageRepository[F] =
    new MessageRepository(ctx)
