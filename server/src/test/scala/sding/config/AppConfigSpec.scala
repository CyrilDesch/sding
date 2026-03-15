package sding.config

import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

class AppConfigSpec extends AsyncWordSpec with AsyncIOSpec with Matchers:

  private val requiredEnv: Map[String, String] = Map(
    "POSTGRES_DATABASE" -> "sding_dev",
    "POSTGRES_USER"     -> "postgres",
    "POSTGRES_PASSWORD" -> "secret"
  )

  "AppConfig" should {
    "load with defaults when only required env vars are set" in {
      AppConfig.loadFrom(requiredEnv).map { config =>
        config.app.name shouldBe "sding"
        config.app.port.value shouldBe 8080
        config.app.debug shouldBe false
        config.app.apiPrefix shouldBe "/api"
        config.postgres.database shouldBe "sding_dev"
        config.postgres.host shouldBe "localhost"
        config.postgres.port shouldBe 5432
        config.postgres.poolSize shouldBe 20
        config.jwt.accessTokenExpireSeconds shouldBe (86400L * 30)
        config.logging.level shouldBe "INFO"
        config.logging.format shouldBe "json"
      }
    }

    "override defaults with custom env values" in {
      val custom = requiredEnv ++ Map(
        "APP_NAME"   -> "custom-app",
        "PORT"       -> "9090",
        "DEBUG"      -> "true",
        "LOG_LEVEL"  -> "DEBUG",
        "LOG_FORMAT" -> "console"
      )
      AppConfig.loadFrom(custom).map { config =>
        config.app.name shouldBe "custom-app"
        config.app.port.value shouldBe 9090
        config.app.debug shouldBe true
        config.logging.level shouldBe "DEBUG"
        config.logging.format shouldBe "console"
      }
    }

    "parse comma-separated CORS origins" in {
      val custom = requiredEnv + ("ALLOWED_ORIGINS" -> "http://localhost:3000,http://localhost:5173")
      AppConfig.loadFrom(custom).map { config =>
        config.cors.allowedOrigins shouldBe List("http://localhost:3000", "http://localhost:5173")
      }
    }

    "parse comma-separated rate limit defaults" in {
      val custom = requiredEnv + ("RATE_LIMIT_DEFAULTS" -> "100 per day,10 per hour")
      AppConfig.loadFrom(custom).map { config =>
        config.rateLimit.defaults shouldBe List("100 per day", "10 per hour")
      }
    }

    "redact postgres password so it is not printed" in {
      AppConfig.loadFrom(requiredEnv).map { config =>
        config.postgres.password.toString should not include "secret"
      }
    }

    "use default sding database when POSTGRES_DATABASE is not set" in {
      val incomplete = requiredEnv - "POSTGRES_DATABASE"
      AppConfig.loadFrom(incomplete).map { config =>
        config.postgres.database shouldBe "sding"
      }
    }

    "fail when PORT is not a valid integer" in {
      val bad = requiredEnv + ("PORT" -> "not-a-number")
      AppConfig.loadFrom(bad).attempt.map { result =>
        result.isLeft shouldBe true
      }
    }
  }
