package sding.service

import cats.effect.Sync
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

trait EncryptionService[F[_]]:
  def encrypt(plaintext: String): F[String]
  def decrypt(ciphertext: String): F[String]

object EncryptionService:
  def make[F[_]: Sync](secretKey: String): EncryptionService[F] =
    val keyBytes = secretKey.getBytes("UTF-8").padTo(32, 0.toByte).take(32)
    val keySpec  = new SecretKeySpec(keyBytes, "AES")

    new EncryptionService[F]:
      def encrypt(plaintext: String): F[String] = Sync[F].delay {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec)
        val iv        = cipher.getIV
        val encrypted = cipher.doFinal(plaintext.getBytes("UTF-8"))
        Base64.getEncoder.encodeToString(iv ++ encrypted)
      }

      def decrypt(ciphertext: String): F[String] = Sync[F].delay {
        val decoded   = Base64.getDecoder.decode(ciphertext)
        val iv        = decoded.take(12)
        val encrypted = decoded.drop(12)
        val cipher    = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(128, iv))
        new String(cipher.doFinal(encrypted), "UTF-8")
      }
