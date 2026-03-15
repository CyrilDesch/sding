package sding.config

import cats.effect.IO
import cats.syntax.all.*
import ciris.*
import com.comcast.ip4s.Host
import com.comcast.ip4s.Port

final case class AppConfig(
    app: AppSettings,
    cors: CorsSettings,
    jwt: JwtSettings,
    encryption: EncryptionSettings,
    postgres: PostgresSettings,
    rateLimit: RateLimitSettings,
    search: SearchSettings,
    logging: LoggingSettings,
    langfuse: LangfuseSettings
)

final case class AppSettings(
    name: String,
    version: String,
    apiPrefix: String,
    debug: Boolean,
    host: Host,
    port: Port
)

final case class CorsSettings(
    allowedOrigins: List[String]
)

final case class JwtSettings(
    secret: Secret[String],
    accessTokenExpireSeconds: Long
)

final case class EncryptionSettings(
    llmEncryptionKey: Secret[String]
)

final case class PostgresSettings(
    host: String,
    port: Int,
    database: String,
    user: String,
    password: Secret[String],
    poolSize: Int,
    maxOverflow: Int
)

final case class RateLimitSettings(
    defaults: List[String]
)

final case class SearchSettings(
    langsearchApiKey: Secret[String],
    exaApiKey: Secret[String]
)

final case class LoggingSettings(
    level: String,
    format: String
)

final case class LangfuseSettings(
    enabled: Boolean,
    baseUrl: String,
    publicKey: Secret[String],
    secretKey: Secret[String]
)

object AppConfig:

  private given ConfigDecoder[String, Host] =
    ConfigDecoder[String, String].mapEither { (_, s) =>
      Host.fromString(s).toRight(ConfigError("Invalid host"))
    }

  private given ConfigDecoder[String, Port] =
    ConfigDecoder[String, Int].mapEither { (_, i) =>
      Port.fromInt(i).toRight(ConfigError("Invalid port"))
    }

  private def lookup(source: String => Option[String])(key: String): ConfigValue[Effect, String] =
    val k = ConfigKey(s"environment variable $key")
    source(key) match
      case Some(value) => ConfigValue.loaded(k, value)
      case None        => ConfigValue.missing(k)

  private def build(get: String => ConfigValue[Effect, String]): ConfigValue[Effect, AppConfig] =
    val appSettings =
      (
        get("APP_NAME").default("sding"),
        get("APP_VERSION").default("0.1.0"),
        get("API_PREFIX").default("/api"),
        get("DEBUG").as[Boolean].default(false),
        get("HOST").as[Host].default(Host.fromString("0.0.0.0").get),
        get("PORT").as[Port].default(Port.fromInt(8080).get)
      ).parMapN(AppSettings.apply)

    val corsSettings =
      get("ALLOWED_ORIGINS")
        .default("*")
        .map(s => CorsSettings(s.split(",").map(_.trim).toList))

    val jwtSettings =
      (
        get("JWT_SECRET").default("change-me-in-production").secret,
        get("JWT_TOKEN_EXPIRE_SECONDS").as[Long].default(86400L * 30)
      ).parMapN(JwtSettings.apply)

    val encryptionSettings =
      get("LLM_ENCRYPTION_KEY").default("change-me-in-production-32-bytes").secret.map(EncryptionSettings.apply)

    val postgresSettings =
      (
        get("POSTGRES_HOST").default("localhost"),
        get("POSTGRES_PORT").as[Int].default(5432),
        get("POSTGRES_DATABASE").default("sding"),
        get("POSTGRES_USER").default("sding"),
        get("POSTGRES_PASSWORD").default("change-me-in-production").secret,
        get("POSTGRES_POOL_SIZE").as[Int].default(20),
        get("POSTGRES_MAX_OVERFLOW").as[Int].default(10)
      ).parMapN(PostgresSettings.apply)

    val rateLimitSettings =
      get("RATE_LIMIT_DEFAULTS")
        .default("200 per day,50 per hour")
        .map(s => RateLimitSettings(s.split(",").map(_.trim).toList))

    val searchSettings =
      (
        get("LANGSEARCH_API_KEY").default("").secret,
        get("EXA_API_KEY").default("").secret
      ).parMapN(SearchSettings.apply)

    val loggingSettings =
      (
        get("LOG_LEVEL").default("INFO"),
        get("LOG_FORMAT").default("json")
      ).parMapN(LoggingSettings.apply)

    val langfuseSettings =
      (
        get("LANGFUSE_ENABLED").as[Boolean].default(true),
        get("LANGFUSE_BASE_URL").default("http://localhost:3000"),
        get("LANGFUSE_PUBLIC_KEY").default("pk-lf-sding-local-public-key").secret,
        get("LANGFUSE_SECRET_KEY").default("sk-lf-sding-local-secret-key").secret
      ).parMapN(LangfuseSettings.apply)

    (
      appSettings,
      corsSettings,
      jwtSettings,
      encryptionSettings,
      postgresSettings,
      rateLimitSettings,
      searchSettings,
      loggingSettings,
      langfuseSettings
    ).parMapN(AppConfig.apply)

  val config: ConfigValue[Effect, AppConfig] =
    build(key => env(key))

  def loadFrom(envMap: Map[String, String]): IO[AppConfig] =
    build(key => lookup(envMap.get)(key)).load[IO]
