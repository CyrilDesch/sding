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

final case class HumanComprehensiveSelectionResult(selectedVariantId: Option[Int] = None)
    derives Decoder,
      Encoder.AsObject,
      JsonSchemaOf

final class HumanComprehensiveSelectionTask[F[_]: Async](
    chatContext: ChatContext[F]
) extends TaskNode[F]:
  val name = WorkflowStep.HumanComprehensiveSelection

  def execute(state: ProjectContextState): F[ProjectContextState] =
    for
      _      <- chatContext.sendMessage("Review the comprehensive analysis results.")
      choice <- chatContext.requestInput(
        Choice("Would you like to select a variant or refine?", List("select", "refine"))
      )
      result <-
        if choice == "refine" then
          for feedback <- chatContext.requestInput(FreeText("What should be refined?"))
          yield state.appendFeedback("comprehensive_analysis_feedback", feedback)
        else
          for
            idStr <- chatContext.requestInput(FreeText("Enter the variant ID to select:"))
            id    <- Async[F].fromOption(
              idStr.toIntOption,
              sding.domain.AppError.ChatError.InvalidInput("Expected a number")
            )
          yield state
            .clearFeedback("comprehensive_analysis_feedback")
            .copy(humanComprehensiveSelectionResult = Some(HumanComprehensiveSelectionResult(Some(id))))
    yield result
