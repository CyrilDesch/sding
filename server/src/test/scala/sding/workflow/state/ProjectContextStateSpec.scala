package sding.workflow.state

import io.circe.parser.decode
import io.circe.syntax.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import sding.protocol.WorkflowStep
import sding.workflow.result.*

class ProjectContextStateSpec extends AnyWordSpec with Matchers:

  "ProjectContextState" should {

    "be created with sensible defaults" in {
      val state = ProjectContextState()
      state.workflowName.shouldBe("")
      state.chatIdStr.shouldBe(None)
      state.errorMessage.shouldBe(None)
      state.goBackFeedbacks.shouldBe(Map.empty)
      state.iterationCount.shouldBe(Map.empty)
      state.projectRequirements.shouldBe(None)
      state.isFinalNode.shouldBe(false)
    }

    "increment iteration count correctly" in {
      val state          = ProjectContextState()
      val (state1, cnt1) = state.incrementIteration(WorkflowStep.WeirdProblemGeneration)
      cnt1.shouldBe(1)
      state1.iterationCount(WorkflowStep.WeirdProblemGeneration.snakeName).shouldBe(1)

      val (state2, cnt2) = state1.incrementIteration(WorkflowStep.WeirdProblemGeneration)
      cnt2.shouldBe(2)
      state2.iterationCount(WorkflowStep.WeirdProblemGeneration.snakeName).shouldBe(2)
    }

    "track multiple node iterations independently" in {
      val state           = ProjectContextState()
      val (state1, _)     = state.incrementIteration(WorkflowStep.TrendAnalysis)
      val (state2, _)     = state1.incrementIteration(WorkflowStep.EmpathyMap)
      val (state3, cnt_a) = state2.incrementIteration(WorkflowStep.TrendAnalysis)
      cnt_a.shouldBe(2)
      state3.iterationCount(WorkflowStep.TrendAnalysis.snakeName).shouldBe(2)
      state3.iterationCount(WorkflowStep.EmpathyMap.snakeName).shouldBe(1)
    }

    "detect feedback presence correctly" in {
      val state = ProjectContextState()
      state.hasFeedback("desired_modification").shouldBe(false)

      val withFeedback = state.appendFeedback("desired_modification", "try harder")
      withFeedback.hasFeedback("desired_modification").shouldBe(true)
    }

    "append multiple feedbacks to the same key" in {
      val state  = ProjectContextState()
      val state1 = state.appendFeedback("desired_modification", "feedback 1")
      val state2 = state1.appendFeedback("desired_modification", "feedback 2")
      state2.goBackFeedbacks("desired_modification").shouldBe(List("feedback 1", "feedback 2"))
    }

    "clear feedback by key" in {
      val state   = ProjectContextState().appendFeedback("desired_modification", "test")
      val cleared = state.clearFeedback("desired_modification")
      cleared.hasFeedback("desired_modification").shouldBe(false)
      cleared.goBackFeedbacks.contains("desired_modification").shouldBe(false)
    }

    "round-trip through JSON" in {
      val state = ProjectContextState(
        workflowName = "test-workflow",
        projectLanguage = Some("French"),
        projectRequirements = Some(
          ProjectRequirements(
            projectType = "B2B",
            projectSoftwareType = "SaaS"
          )
        )
      )
      val json    = state.asJson
      val decoded = decode[ProjectContextState](json.noSpaces)
      decoded.toOption.map(_.workflowName).shouldBe(Some("test-workflow"))
      decoded.toOption.flatMap(_.projectLanguage).shouldBe(Some("French"))
      decoded.toOption.flatMap(_.projectRequirements).map(_.projectType).shouldBe(Some("B2B"))
    }
  }
