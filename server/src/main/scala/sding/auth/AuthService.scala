package sding.auth

import cats.effect.Sync
import cats.syntax.all.*
import io.circe.Json
import java.time.Instant
import pdi.jwt.JwtAlgorithm
import pdi.jwt.JwtCirce
import pdi.jwt.JwtClaim
import sding.domain.AppError
import sding.domain.UserId
import sding.repository.UserRepository

final case class AuthUser(id: UserId, email: String, role: String)
final case class AuthToken(token: String, expiresAt: Instant)

trait AuthService[F[_]]:
  def register(email: String, password: String, firstName: String, lastName: String): F[AuthToken]
  def login(email: String, password: String): F[AuthToken]
  def verifyToken(token: String): F[AuthUser]

object AuthService:
  def make[F[_]: Sync](userRepo: UserRepository[F], jwtSecret: String, tokenExpirySeconds: Long): AuthService[F] =
    new AuthService[F]:
      private val algorithm = JwtAlgorithm.HS256

      def register(email: String, password: String, firstName: String, lastName: String): F[AuthToken] =
        for
          existing <- userRepo.findByEmail(email)
          _        <- existing.fold(Sync[F].unit)(_ =>
            Sync[F].raiseError(AppError.AuthError.InvalidCredentials("Email already registered"))
          )
          hash <- Sync[F].blocking(
            at.favre.lib.crypto.bcrypt.BCrypt.withDefaults().hashToString(12, password.toCharArray)
          )
          user  <- userRepo.create(email, hash, firstName, lastName)
          token <- generateToken(user.id, user.email, user.role)
        yield token

      def login(email: String, password: String): F[AuthToken] =
        for
          userOpt <- userRepo.findByEmail(email)
          user    <- userOpt.fold(
            Sync[F].raiseError[sding.repository.UserRecord](
              AppError.AuthError.InvalidCredentials("Invalid email or password")
            )
          )(Sync[F].pure)
          hash <- user.passwordHash.fold(
            Sync[F].raiseError[String](AppError.AuthError.InvalidCredentials("No password set"))
          )(Sync[F].pure)
          valid <- Sync[F].blocking(
            at.favre.lib.crypto.bcrypt.BCrypt.verifyer().verify(password.toCharArray, hash).verified
          )
          _ <-
            if valid then Sync[F].unit
            else Sync[F].raiseError(AppError.AuthError.InvalidCredentials("Invalid email or password"))
          token <- generateToken(user.id, user.email, user.role)
        yield token

      def verifyToken(token: String): F[AuthUser] =
        Sync[F]
          .fromTry(JwtCirce.decode(token, jwtSecret, Seq(algorithm)))
          .flatMap { claim =>
            val json   = io.circe.parser.parse(claim.content).getOrElse(Json.Null)
            val cursor = json.hcursor
            (cursor.get[String]("sub"), cursor.get[String]("email"), cursor.get[String]("role")) match
              case (Right(sub), Right(em), Right(ro)) =>
                Sync[F].pure(AuthUser(UserId.fromString(sub), em, ro))
              case _ =>
                Sync[F].raiseError(AppError.AuthError.TokenExpired("Invalid token payload"))
          }
          .handleErrorWith(_ => Sync[F].raiseError(AppError.AuthError.TokenExpired("Invalid or expired token")))

      private def generateToken(userId: UserId, email: String, role: String): F[AuthToken] =
        Sync[F].delay {
          val now   = Instant.now()
          val exp   = now.plusSeconds(tokenExpirySeconds)
          val claim = JwtClaim(
            content = Json
              .obj(
                "sub"   -> Json.fromString(userId.asString),
                "email" -> Json.fromString(email),
                "role"  -> Json.fromString(role)
              )
              .noSpaces,
            issuedAt = Some(now.getEpochSecond),
            expiration = Some(exp.getEpochSecond)
          )
          AuthToken(JwtCirce.encode(claim, jwtSecret, algorithm), exp)
        }
