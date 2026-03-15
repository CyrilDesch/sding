package sding.repository

import cats.effect.Sync
import io.getquill.*
import java.time.Instant
import java.util.UUID
import sding.domain.ChatId
import sding.domain.MessageId
import sding.domain.UserId

final case class MessageRecord(
    id: MessageId,
    chatId: ChatId,
    senderId: Option[UserId],
    senderType: SenderType,
    content: String,
    contentType: ContentType,
    sourceNode: Option[String],
    createdAt: Instant
)

final class MessageRepository[F[_]: Sync](ctx: PostgresJdbcContext[SnakeCase]):
  import ctx.*

  private given MappedEncoding[MessageId, UUID] = MappedEncoding(_.value)
  private given MappedEncoding[UUID, MessageId] = MappedEncoding(MessageId.apply)
  private given MappedEncoding[ChatId, UUID]    = MappedEncoding(_.value)
  private given MappedEncoding[UUID, ChatId]    = MappedEncoding(ChatId.apply)
  private given MappedEncoding[UserId, UUID]    = MappedEncoding(_.value)
  private given MappedEncoding[UUID, UserId]    = MappedEncoding(UserId.apply)

  private given MappedEncoding[SenderType, String] = MappedEncoding {
    case SenderType.User   => "USER"
    case SenderType.System => "SYSTEM"
    case SenderType.Agent  => "AGENT"
  }
  private given MappedEncoding[String, SenderType] = MappedEncoding {
    case "USER"   => SenderType.User
    case "SYSTEM" => SenderType.System
    case "AGENT"  => SenderType.Agent
    case s        => throw new IllegalArgumentException(s"Unknown sender_type: $s")
  }

  private given MappedEncoding[ContentType, String] = MappedEncoding {
    case ContentType.Text     => "TEXT"
    case ContentType.Markdown => "MARKDOWN"
    case ContentType.Html     => "HTML"
  }
  private given MappedEncoding[String, ContentType] = MappedEncoding {
    case "TEXT"     => ContentType.Text
    case "MARKDOWN" => ContentType.Markdown
    case "HTML"     => ContentType.Html
    case s          => throw new IllegalArgumentException(s"Unknown content_type: $s")
  }

  inline given SchemaMeta[MessageRecord] = schemaMeta("messages")

  def create(
      cid: ChatId,
      senderType: SenderType,
      content: String,
      contentType: ContentType,
      sourceNode: Option[String]
  ): F[MessageRecord] = Sync[F].blocking {
    val record = MessageRecord(
      id = MessageId.random,
      chatId = cid,
      senderId = None,
      senderType = senderType,
      content = content,
      contentType = contentType,
      sourceNode = sourceNode,
      createdAt = Instant.now()
    )
    run(query[MessageRecord].insertValue(lift(record)))
    record
  }

  def findByChat(cid: ChatId, limit: Int, offset: Int): F[List[MessageRecord]] = Sync[F].blocking {
    run(
      query[MessageRecord]
        .filter(_.chatId == lift(cid))
        .sortBy(_.createdAt)(using Ord.asc)
        .drop(lift(offset))
        .take(lift(limit))
    )
  }

object MessageRepository:
  def make[F[_]: Sync](ctx: PostgresJdbcContext[SnakeCase]): MessageRepository[F] =
    new MessageRepository(ctx)
