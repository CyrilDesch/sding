package sding.protocol

import io.circe.Decoder
import io.circe.Encoder

enum LlmProvider:
  case Gemini, OpenAI, Anthropic

object LlmProvider:
  given Encoder[LlmProvider] = Encoder.encodeString.contramap(_.toString)
  given Decoder[LlmProvider] = Decoder.decodeString.emap { s =>
    LlmProvider.values.find(_.toString.equalsIgnoreCase(s)).toRight(s"Unknown LLM provider: $s")
  }

final case class LlmConfigResponse(
    provider: LlmProvider,
    model: String,
    keyHint: String
) derives Decoder,
      Encoder.AsObject

final case class LlmConfigRequest(
    provider: LlmProvider,
    apiKey: String,
    model: String
) derives Decoder,
      Encoder.AsObject
