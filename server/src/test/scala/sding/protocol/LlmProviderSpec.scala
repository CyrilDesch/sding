package sding.protocol

import io.circe.parser.decode
import io.circe.syntax.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class LlmProviderSpec extends AnyWordSpec with Matchers:

  "LlmProvider" should {
    "decode and encode provider names case-insensitively" in {
      decode[LlmProvider]("\"gemini\"") shouldBe Right(LlmProvider.Gemini)
      decode[LlmProvider]("\"openai\"") shouldBe Right(LlmProvider.OpenAI)
      decode[LlmProvider]("\"anthropic\"") shouldBe Right(LlmProvider.Anthropic)
      LlmProvider.Gemini.asJson.noSpaces shouldBe "\"Gemini\""
      decode[LlmProvider](LlmProvider.Gemini.asJson.noSpaces) shouldBe Right(LlmProvider.Gemini)
    }
  }
