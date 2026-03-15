package sding.service

import cats.effect.Sync
import org.flywaydb.core.Flyway
import sding.config.PostgresSettings

object DatabaseMigrator:

  def migrate[F[_]: Sync](config: PostgresSettings): F[Int] =
    Sync[F].blocking {
      val flyway = Flyway
        .configure()
        .dataSource(
          s"jdbc:postgresql://${config.host}:${config.port}/${config.database}?stringtype=unspecified",
          config.user,
          config.password.value
        )
        .locations("classpath:db/migration")
        .load()
      flyway.migrate().migrationsExecuted
    }
