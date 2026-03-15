package sding.domain

import io.circe.parser.decode
import io.circe.syntax.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ModelsSpec extends AnyWordSpec with Matchers:

  "UserRole" should {
    "encode Admin to JSON as ADMIN" in {
      UserRole.Admin.asJson.noSpaces shouldBe "\"ADMIN\""
    }

    "encode User to JSON as USER" in {
      UserRole.User.asJson.noSpaces shouldBe "\"USER\""
    }

    "decode ADMIN from JSON" in {
      decode[UserRole]("\"ADMIN\"") shouldBe Right(UserRole.Admin)
    }

    "decode USER from JSON" in {
      decode[UserRole]("\"USER\"") shouldBe Right(UserRole.User)
    }

    "fail to decode an unknown role" in {
      decode[UserRole]("\"SUPERADMIN\"").isLeft shouldBe true
    }
  }

  "ProjectStatus" should {
    "round-trip all variants through JSON" in {
      val statuses = List(
        ProjectStatus.Draft      -> "\"draft\"",
        ProjectStatus.InProgress -> "\"in_progress\"",
        ProjectStatus.Completed  -> "\"completed\"",
        ProjectStatus.Archived   -> "\"archived\""
      )
      statuses.foreach { (status, expectedJson) =>
        status.asJson.noSpaces shouldBe expectedJson
        decode[ProjectStatus](expectedJson) shouldBe Right(status)
      }
    }

    "fail to decode unknown status" in {
      decode[ProjectStatus]("\"cancelled\"").isLeft shouldBe true
    }
  }

  "SenderType" should {
    "round-trip all variants through JSON" in {
      val types = List(
        SenderType.User   -> "\"USER\"",
        SenderType.System -> "\"SYSTEM\"",
        SenderType.Agent  -> "\"AGENT\""
      )
      types.foreach { (st, expectedJson) =>
        st.asJson.noSpaces shouldBe expectedJson
        decode[SenderType](expectedJson) shouldBe Right(st)
      }
    }
  }

  "ContentType" should {
    "round-trip all variants through JSON" in {
      val types = List(
        ContentType.Text     -> "\"TEXT\"",
        ContentType.Markdown -> "\"MARKDOWN\"",
        ContentType.Html     -> "\"HTML\""
      )
      types.foreach { (ct, expectedJson) =>
        ct.asJson.noSpaces shouldBe expectedJson
        decode[ContentType](expectedJson) shouldBe Right(ct)
      }
    }
  }

  "MessageType" should {
    "round-trip all variants through JSON" in {
      val types = List(
        MessageType.Message         -> "\"message\"",
        MessageType.StateUpdate     -> "\"state_update\"",
        MessageType.InputRequest    -> "\"input_request\"",
        MessageType.InputSubmission -> "\"input_submission\"",
        MessageType.Error           -> "\"error\""
      )
      types.foreach { (mt, expectedJson) =>
        mt.asJson.noSpaces shouldBe expectedJson
        decode[MessageType](expectedJson) shouldBe Right(mt)
      }
    }
  }

  "MessageFormat" should {
    "round-trip all variants through JSON" in {
      val formats = List(
        MessageFormat.Text     -> "\"text\"",
        MessageFormat.Html     -> "\"html\"",
        MessageFormat.Markdown -> "\"markdown\""
      )
      formats.foreach { (mf, expectedJson) =>
        mf.asJson.noSpaces shouldBe expectedJson
        decode[MessageFormat](expectedJson) shouldBe Right(mf)
      }
    }
  }

  "InputType" should {
    "round-trip all variants through JSON" in {
      val types = List(
        InputType.Text           -> "\"text\"",
        InputType.Choice         -> "\"choice\"",
        InputType.MultipleChoice -> "\"multiple_choice\"",
        InputType.Number         -> "\"number\"",
        InputType.File           -> "\"file\""
      )
      types.foreach { (it, expectedJson) =>
        it.asJson.noSpaces shouldBe expectedJson
        decode[InputType](expectedJson) shouldBe Right(it)
      }
    }

    "fail to decode unknown input type" in {
      decode[InputType]("\"slider\"").isLeft shouldBe true
    }
  }
