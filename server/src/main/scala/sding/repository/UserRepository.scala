package sding.repository

import cats.effect.Sync
import io.getquill.*
import java.util.UUID
import sding.domain.UserId
import sding.protocol.LlmProvider

final case class UserRecord(
    id: UserId,
    email: String,
    passwordHash: Option[String],
    firstName: String,
    lastName: String,
    role: String,
    llmProvider: Option[LlmProvider],
    encryptedApiKey: Option[String],
    llmModel: Option[String]
)

final class UserRepository[F[_]: Sync](ctx: PostgresJdbcContext[SnakeCase]):
  import ctx.*

  private given MappedEncoding[UserId, UUID]        = MappedEncoding(_.value)
  private given MappedEncoding[UUID, UserId]        = MappedEncoding(UserId.apply)
  private given MappedEncoding[LlmProvider, String] = MappedEncoding(_.toString)
  private given MappedEncoding[String, LlmProvider] = MappedEncoding { s =>
    LlmProvider.values
      .find(_.toString == s)
      .getOrElse(throw new IllegalArgumentException(s"Unknown LlmProvider in DB: '$s'. Valid values: ${LlmProvider.values.mkString(", ")}"))
  }

  inline given SchemaMeta[UserRecord] = schemaMeta("users")

  def findById(id: UserId): F[Option[UserRecord]] = Sync[F].blocking {
    run(query[UserRecord].filter(_.id == lift(id))).headOption
  }

  def findByEmail(email: String): F[Option[UserRecord]] = Sync[F].blocking {
    run(query[UserRecord].filter(_.email == lift(email))).headOption
  }

  def create(email: String, passwordHash: String, firstName: String, lastName: String): F[UserRecord] =
    Sync[F].blocking {
      val record = UserRecord(
        id = UserId.random,
        email = email,
        passwordHash = Some(passwordHash),
        firstName = firstName,
        lastName = lastName,
        role = "USER",
        llmProvider = None,
        encryptedApiKey = None,
        llmModel = None
      )
      run(query[UserRecord].insertValue(lift(record)))
      record
    }

  def updateLlmConfig(id: UserId, provider: LlmProvider, encryptedApiKey: String, model: String): F[Unit] =
    Sync[F].blocking {
      run(
        query[UserRecord]
          .filter(_.id == lift(id))
          .update(
            _.llmProvider     -> lift(Option(provider)),
            _.encryptedApiKey -> lift(Option(encryptedApiKey)),
            _.llmModel        -> lift(Option(model))
          )
      )
      ()
    }

  def getLlmConfig(id: UserId): F[Option[(LlmProvider, String, String)]] = Sync[F].blocking {
    run(query[UserRecord].filter(_.id == lift(id))).headOption.flatMap { u =>
      for
        p <- u.llmProvider
        k <- u.encryptedApiKey
        m <- u.llmModel
      yield (p, k, m)
    }
  }

object UserRepository:
  def make[F[_]: Sync](ctx: PostgresJdbcContext[SnakeCase]): UserRepository[F] =
    new UserRepository(ctx)
