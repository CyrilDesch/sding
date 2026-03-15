package sding.workflow.task

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import sding.protocol.SelectionItem
import sding.workflow.io.ChatContext
import sding.workflow.io.MessageFormat
import sding.workflow.io.UserInputRequest
import sding.workflow.result.*
import sding.workflow.state.ProjectContextState

class HumanTaskSpec extends AsyncWordSpec with AsyncIOSpec with Matchers:

  private def scriptedChatContext(responses: List[String]): ChatContext[IO] =
    new ChatContext[IO]:
      private var remaining                                             = responses
      def sendMessage(message: String, format: MessageFormat): IO[Unit] = IO.unit
      def sendState(message: String): IO[Unit]                          = IO.unit
      def requestInput(request: UserInputRequest): IO[String]           =
        IO {
          val resp = remaining.headOption.getOrElse("default")
          remaining = remaining.drop(1)
          resp
        }
      def requestSelection(title: String, items: List[SelectionItem], allowRetry: Boolean): IO[String] =
        IO {
          val resp = remaining.headOption.getOrElse("default")
          remaining = remaining.drop(1)
          resp
        }

  "HumanProblemSelectionTask" should {

    "set feedback on retry" in {
      val chat = scriptedChatContext(List("retry", "make it more creative"))
      val task = HumanProblemSelectionTask[IO](chat)

      task.execute(ProjectContextState()).asserting { result =>
        result.hasFeedback("desired_modification") shouldBe true
        result.goBackFeedbacks("desired_modification") shouldBe List("make it more creative")
        result.humanProblemSelectionResult shouldBe None
      }
    }

    "select a problem by ID" in {
      val chat = scriptedChatContext(List("2"))
      val task = HumanProblemSelectionTask[IO](chat)

      task.execute(ProjectContextState()).asserting { result =>
        result.hasFeedback("desired_modification") shouldBe false
        result.humanProblemSelectionResult shouldBe Some(HumanGateResult(2))
      }
    }
  }

  "HumanJTBDSelectionTask" should {

    "set feedback on retry" in {
      val chat = scriptedChatContext(List("retry", "add more detail"))
      val task = HumanJTBDSelectionTask[IO](chat)

      task.execute(ProjectContextState()).asserting { result =>
        result.hasFeedback("jtbd_improvement_feedback") shouldBe true
        result.humanJtbdSelectionResult shouldBe None
      }
    }

    "select a JTBD on select" in {
      val chat = scriptedChatContext(List("select", "3"))
      val task = HumanJTBDSelectionTask[IO](chat)

      task.execute(ProjectContextState()).asserting { result =>
        result.humanJtbdSelectionResult shouldBe Some(HumanJTBDSelectionResult(3))
      }
    }
  }

  "HumanComprehensiveSelectionTask" should {

    "set feedback on refine" in {
      val chat = scriptedChatContext(List("refine", "need better analysis"))
      val task = HumanComprehensiveSelectionTask[IO](chat)

      task.execute(ProjectContextState()).asserting { result =>
        result.hasFeedback("comprehensive_analysis_feedback") shouldBe true
        result.humanComprehensiveSelectionResult shouldBe None
      }
    }

    "select a variant on select" in {
      val chat = scriptedChatContext(List("select", "5"))
      val task = HumanComprehensiveSelectionTask[IO](chat)

      task.execute(ProjectContextState()).asserting { result =>
        result.humanComprehensiveSelectionResult shouldBe Some(HumanComprehensiveSelectionResult(Some(5)))
      }
    }
  }

  "HumanProjectSelectionTask" should {

    "set feedback on no" in {
      val chat = scriptedChatContext(List("no", "revise the features"))
      val task = HumanProjectSelectionTask[IO](chat)

      task.execute(ProjectContextState()).asserting { result =>
        result.hasFeedback("human_project_revision_feedback") shouldBe true
        result.humanProjectSelectionResult shouldBe None
      }
    }

    "select a project on yes" in {
      val chat = scriptedChatContext(List("yes", "1"))
      val task = HumanProjectSelectionTask[IO](chat)

      task.execute(ProjectContextState()).asserting { result =>
        result.humanProjectSelectionResult shouldBe Some(HumanProjectSelectionResult(1))
      }
    }
  }
