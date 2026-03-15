package sding.client

import io.circe.parser.decode
import io.circe.syntax.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import sding.protocol.*

class ProtocolSpec extends AnyWordSpec with Matchers:

  "CreateChatResponse" should {
    "round-trip through JSON" in {
      val resp = CreateChatResponse(chatId = "chat-123")
      decode[CreateChatResponse](resp.asJson.noSpaces) shouldBe Right(resp)
    }
  }
