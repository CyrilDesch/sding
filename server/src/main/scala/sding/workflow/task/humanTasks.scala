package sding.workflow.task

import cats.effect.Async
import cats.syntax.all.*
import sding.workflow.io.ChatContext
import sding.workflow.result.*
import sding.workflow.state.ProjectContextState

final class HumanProblemSelectionTask[F[_]: Async](
    chatContext: ChatContext[F]
) extends TaskNode[F]:
  val name = "human_problem_selection"

  def execute(state: ProjectContextState): F[ProjectContextState] =
    for
      _      <- chatContext.sendMessage("Review the generated problems and evidence.")
      choice <- chatContext.requestInput(
        "Would you like to select a problem or retry?",
        Some(List("select", "retry"))
      )
      result <-
        if choice == "retry" then
          for feedback <- chatContext.requestInput("What changes would you like?")
          yield state.appendFeedback("desired_modification", feedback)
        else
          for
            idStr <- chatContext.requestInput("Enter the problem ID to select:")
            id    <- Async[F].fromOption(
              idStr.toIntOption,
              sding.domain.AppError.ChatError.InvalidInput("Expected a number")
            )
          yield state
            .clearFeedback("desired_modification")
            .copy(humanProblemSelectionResult = Some(HumanGateResult(id)))
    yield result

final class HumanJTBDSelectionTask[F[_]: Async](
    chatContext: ChatContext[F]
) extends TaskNode[F]:
  val name = "human_jtbd_selection"

  def execute(state: ProjectContextState): F[ProjectContextState] =
    for
      _      <- chatContext.sendMessage("Review the jobs-to-be-done.")
      choice <- chatContext.requestInput(
        "Would you like to select a JTBD or retry?",
        Some(List("select", "retry"))
      )
      result <-
        if choice == "retry" then
          for feedback <- chatContext.requestInput("What should be improved?")
          yield state.appendFeedback("jtbd_improvement_feedback", feedback)
        else
          for
            idStr <- chatContext.requestInput("Enter the JTBD ID to select:")
            id    <- Async[F].fromOption(
              idStr.toIntOption,
              sding.domain.AppError.ChatError.InvalidInput("Expected a number")
            )
          yield state
            .clearFeedback("jtbd_improvement_feedback")
            .copy(humanJtbdSelectionResult = Some(HumanJTBDSelectionResult(id)))
    yield result

final class HumanComprehensiveSelectionTask[F[_]: Async](
    chatContext: ChatContext[F]
) extends TaskNode[F]:
  val name = "human_comprehensive_selection"

  def execute(state: ProjectContextState): F[ProjectContextState] =
    for
      _      <- chatContext.sendMessage("Review the comprehensive analysis results.")
      choice <- chatContext.requestInput(
        "Would you like to select a variant or refine?",
        Some(List("select", "refine"))
      )
      result <-
        if choice == "refine" then
          for feedback <- chatContext.requestInput("What should be refined?")
          yield state.appendFeedback("comprehensive_analysis_feedback", feedback)
        else
          for
            idStr <- chatContext.requestInput("Enter the variant ID to select:")
            id    <- Async[F].fromOption(
              idStr.toIntOption,
              sding.domain.AppError.ChatError.InvalidInput("Expected a number")
            )
          yield state
            .clearFeedback("comprehensive_analysis_feedback")
            .copy(humanComprehensiveSelectionResult = Some(HumanComprehensiveSelectionResult(Some(id))))
    yield result

final class HumanProjectSelectionTask[F[_]: Async](
    chatContext: ChatContext[F]
) extends TaskNode[F]:
  val name = "human_project_selection"

  def execute(state: ProjectContextState): F[ProjectContextState] =
    for
      _      <- chatContext.sendMessage("Review the synthesized project cards.")
      choice <- chatContext.requestInput(
        "Are you satisfied with a project?",
        Some(List("yes", "no"))
      )
      result <-
        if choice == "no" then
          for feedback <- chatContext.requestInput("What should be revised?")
          yield state.appendFeedback("human_project_revision_feedback", feedback)
        else
          for
            idStr <- chatContext.requestInput("Enter the project ID to select:")
            id    <- Async[F].fromOption(
              idStr.toIntOption,
              sding.domain.AppError.ChatError.InvalidInput("Expected a number")
            )
          yield state
            .clearFeedback("human_project_revision_feedback")
            .copy(humanProjectSelectionResult = Some(HumanProjectSelectionResult(id)))
    yield result
