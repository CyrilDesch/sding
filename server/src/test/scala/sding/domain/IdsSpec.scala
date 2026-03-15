package sding.domain

import cats.syntax.all.*
import io.circe.parser.decode
import io.circe.syntax.*
import java.util.UUID
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class IdsSpec extends AnyWordSpec with Matchers:

  private val raw = UUID.fromString("550e8400-e29b-41d4-a716-446655440000")

  "UserId" should {
    "construct from a UUID and expose .value" in {
      val id = UserId(raw)
      id.value shouldBe raw
    }

    "round-trip through circe JSON encoding" in {
      val id      = UserId(raw)
      val json    = id.asJson
      val decoded = json.as[UserId]
      decoded shouldBe Right(id)
    }

    "construct from a string representation" in {
      val id = UserId.fromString("550e8400-e29b-41d4-a716-446655440000")
      id.value shouldBe raw
    }

    "provide a Show instance" in {
      val id = UserId(raw)
      id.show shouldBe "550e8400-e29b-41d4-a716-446655440000"
    }

    "support equality via Eq" in {
      val a = UserId(raw)
      val b = UserId(raw)
      (a === b) shouldBe true
    }

    "generate random unique IDs" in {
      val a = UserId.random
      val b = UserId.random
      (a === b) shouldBe false
    }
  }

  "ProjectId" should {
    "round-trip through circe JSON encoding" in {
      val id      = ProjectId(raw)
      val decoded = decode[ProjectId](id.asJson.noSpaces)
      decoded shouldBe Right(id)
    }
  }

  "ChatId" should {
    "round-trip through circe JSON encoding" in {
      val id      = ChatId(raw)
      val decoded = decode[ChatId](id.asJson.noSpaces)
      decoded shouldBe Right(id)
    }
  }

  "MessageId" should {
    "round-trip through circe JSON encoding" in {
      val id      = MessageId(raw)
      val decoded = decode[MessageId](id.asJson.noSpaces)
      decoded shouldBe Right(id)
    }
  }

  "StepId" should {
    "round-trip through circe JSON encoding" in {
      val id      = StepId(raw)
      val decoded = decode[StepId](id.asJson.noSpaces)
      decoded shouldBe Right(id)
    }
  }

  "DocumentId" should {
    "round-trip through circe JSON encoding" in {
      val id      = DocumentId(raw)
      val decoded = decode[DocumentId](id.asJson.noSpaces)
      decoded shouldBe Right(id)
    }
  }

  "VersionId" should {
    "round-trip through circe JSON encoding" in {
      val id      = VersionId(raw)
      val decoded = decode[VersionId](id.asJson.noSpaces)
      decoded shouldBe Right(id)
    }
  }
