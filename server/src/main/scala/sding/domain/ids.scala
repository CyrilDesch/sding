package sding.domain

import cats.Eq
import cats.Order
import cats.Show
import io.circe.Decoder
import io.circe.Encoder
import java.util.UUID

opaque type UserId = UUID
object UserId:
  inline def apply(v: UUID): UserId        = v
  inline def fromString(s: String): UserId = UUID.fromString(s)
  inline def random: UserId                = UUID.randomUUID()

  extension (id: UserId)
    inline def value: UUID      = id
    inline def asString: String = id.toString

  given Eq[UserId]      = Eq.fromUniversalEquals
  given Order[UserId]   = Order.by(_.value)
  given Show[UserId]    = Show.show(_.asString)
  given Encoder[UserId] = Encoder.encodeUUID.contramap(_.value)
  given Decoder[UserId] = Decoder.decodeUUID.map(UserId.apply)

opaque type ProjectId = UUID
object ProjectId:
  inline def apply(v: UUID): ProjectId        = v
  inline def fromString(s: String): ProjectId = UUID.fromString(s)
  inline def random: ProjectId                = UUID.randomUUID()

  extension (id: ProjectId)
    inline def value: UUID      = id
    inline def asString: String = id.toString

  given Eq[ProjectId]      = Eq.fromUniversalEquals
  given Order[ProjectId]   = Order.by(_.value)
  given Show[ProjectId]    = Show.show(_.asString)
  given Encoder[ProjectId] = Encoder.encodeUUID.contramap(_.value)
  given Decoder[ProjectId] = Decoder.decodeUUID.map(ProjectId.apply)

opaque type ChatId = UUID
object ChatId:
  inline def apply(v: UUID): ChatId        = v
  inline def fromString(s: String): ChatId = UUID.fromString(s)
  inline def random: ChatId                = UUID.randomUUID()

  extension (id: ChatId)
    inline def value: UUID      = id
    inline def asString: String = id.toString

  given Eq[ChatId]      = Eq.fromUniversalEquals
  given Order[ChatId]   = Order.by(_.value)
  given Show[ChatId]    = Show.show(_.asString)
  given Encoder[ChatId] = Encoder.encodeUUID.contramap(_.value)
  given Decoder[ChatId] = Decoder.decodeUUID.map(ChatId.apply)

opaque type MessageId = UUID
object MessageId:
  inline def apply(v: UUID): MessageId        = v
  inline def fromString(s: String): MessageId = UUID.fromString(s)
  inline def random: MessageId                = UUID.randomUUID()

  extension (id: MessageId)
    inline def value: UUID      = id
    inline def asString: String = id.toString

  given Eq[MessageId]      = Eq.fromUniversalEquals
  given Order[MessageId]   = Order.by(_.value)
  given Show[MessageId]    = Show.show(_.asString)
  given Encoder[MessageId] = Encoder.encodeUUID.contramap(_.value)
  given Decoder[MessageId] = Decoder.decodeUUID.map(MessageId.apply)

opaque type StepId = UUID
object StepId:
  inline def apply(v: UUID): StepId        = v
  inline def fromString(s: String): StepId = UUID.fromString(s)
  inline def random: StepId                = UUID.randomUUID()

  extension (id: StepId)
    inline def value: UUID      = id
    inline def asString: String = id.toString

  given Eq[StepId]      = Eq.fromUniversalEquals
  given Order[StepId]   = Order.by(_.value)
  given Show[StepId]    = Show.show(_.asString)
  given Encoder[StepId] = Encoder.encodeUUID.contramap(_.value)
  given Decoder[StepId] = Decoder.decodeUUID.map(StepId.apply)

opaque type DocumentId = UUID
object DocumentId:
  inline def apply(v: UUID): DocumentId        = v
  inline def fromString(s: String): DocumentId = UUID.fromString(s)
  inline def random: DocumentId                = UUID.randomUUID()

  extension (id: DocumentId)
    inline def value: UUID      = id
    inline def asString: String = id.toString

  given Eq[DocumentId]      = Eq.fromUniversalEquals
  given Order[DocumentId]   = Order.by(_.value)
  given Show[DocumentId]    = Show.show(_.asString)
  given Encoder[DocumentId] = Encoder.encodeUUID.contramap(_.value)
  given Decoder[DocumentId] = Decoder.decodeUUID.map(DocumentId.apply)

opaque type VersionId = UUID
object VersionId:
  inline def apply(v: UUID): VersionId        = v
  inline def fromString(s: String): VersionId = UUID.fromString(s)
  inline def random: VersionId                = UUID.randomUUID()

  extension (id: VersionId)
    inline def value: UUID      = id
    inline def asString: String = id.toString

  given Eq[VersionId]      = Eq.fromUniversalEquals
  given Order[VersionId]   = Order.by(_.value)
  given Show[VersionId]    = Show.show(_.asString)
  given Encoder[VersionId] = Encoder.encodeUUID.contramap(_.value)
  given Decoder[VersionId] = Decoder.decodeUUID.map(VersionId.apply)
