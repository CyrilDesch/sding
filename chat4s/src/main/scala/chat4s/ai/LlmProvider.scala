package chat4s.ai

import io.circe.Decoder
import io.circe.Encoder

enum LlmProvider:
  case Gemini, OpenAI, Anthropic, DeepSeek, OpenRouter

object LlmProvider:
  given Encoder[LlmProvider] = Encoder.encodeString.contramap(_.toString)
  given Decoder[LlmProvider] = Decoder.decodeString.emap { s =>
    LlmProvider.values.find(_.toString.equalsIgnoreCase(s)).toRight(s"Unknown LLM provider: $s")
  }
