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

final case class HumanProjectSelectionResult(selectedProjectId: Int) derives Decoder, Encoder.AsObject, JsonSchemaOf

final class HumanProjectSelectionTask[F[_]: Async](
    chatContext: ChatContext[F]
) extends TaskNode[F]:
  val name = WorkflowStep.HumanProjectSelection

  def execute(state: ProjectContextState): F[ProjectContextState] =
    for
      _      <- chatContext.sendMessage("Review the synthesized project cards.")
      choice <- chatContext.requestInput(Choice("Are you satisfied with a project?", List("yes", "no")))
      result <-
        if choice == "no" then
          for feedback <- chatContext.requestInput(FreeText("What should be revised?"))
          yield state.appendFeedback("human_project_revision_feedback", feedback)
        else
          for
            idStr <- chatContext.requestInput(FreeText("Enter the project ID to select:"))
            id    <- Async[F].fromOption(
              idStr.toIntOption,
              sding.domain.AppError.ChatError.InvalidInput("Expected a number")
            )
          yield state
            .clearFeedback("human_project_revision_feedback")
            .copy(humanProjectSelectionResult = Some(HumanProjectSelectionResult(id)))
    yield result
