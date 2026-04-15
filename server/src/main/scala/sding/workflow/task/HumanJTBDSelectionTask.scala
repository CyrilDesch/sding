package sding.workflow.task

import cats.effect.Async
import cats.syntax.all.*
import chat4s.ai.JsonSchemaOf
import chat4s.io.ChatContext
import chat4s.io.UserInputRequest.*
import io.circe.Decoder
import io.circe.Encoder
import sding.protocol.WorkflowStep
import sding.workflow.TaskNode
import sding.workflow.state.ProjectContextState

final case class HumanJTBDSelectionResult(selectedJtbdId: Int) derives Decoder, Encoder.AsObject, JsonSchemaOf

final class HumanJTBDSelectionTask[F[_]: Async](
    chatContext: ChatContext[F]
) extends TaskNode[F]:
  val name = WorkflowStep.HumanJtbdSelection

  def execute(state: ProjectContextState): F[ProjectContextState] =
    for
      _      <- chatContext.sendMessage("Review the jobs-to-be-done.")
      choice <- chatContext.requestInput(Choice("Would you like to select a JTBD or retry?", List("select", "retry")))
      result <-
        if choice == "retry" then
          for feedback <- chatContext.requestInput(FreeText("What should be improved?"))
          yield state.appendFeedback("jtbd_improvement_feedback", feedback)
        else
          for
            idStr <- chatContext.requestInput(FreeText("Enter the JTBD ID to select:"))
            id    <- Async[F].fromOption(
              idStr.toIntOption,
              sding.domain.AppError.ChatError.InvalidInput("Expected a number")
            )
          yield state
            .clearFeedback("jtbd_improvement_feedback")
            .copy(humanJtbdSelectionResult = Some(HumanJTBDSelectionResult(id)))
    yield result
