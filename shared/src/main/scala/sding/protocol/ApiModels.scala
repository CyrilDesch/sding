package sding.protocol

import io.circe.Decoder
import io.circe.Encoder

final case class CreateChatResponse(chatId: String) derives Decoder, Encoder.AsObject
final case class SubmitInputRequest(input: String) derives Decoder, Encoder.AsObject
final case class StatusResponse(status: String) derives Decoder, Encoder.AsObject
final case class ErrorResponse(error: String) derives Decoder, Encoder.AsObject

final case class RegisterRequest(email: String, password: String, firstName: String, lastName: String)
    derives Decoder,
      Encoder.AsObject
final case class LoginRequest(email: String, password: String) derives Decoder, Encoder.AsObject
final case class AuthTokenResponse(token: String, expiresAt: Long) derives Decoder, Encoder.AsObject
