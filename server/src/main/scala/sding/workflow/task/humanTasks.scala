package sding.workflow.task

import cats.effect.Async
import cats.syntax.all.*
import sding.protocol.SelectionDetail
import sding.protocol.SelectionItem
import sding.protocol.WorkflowStep
import sding.workflow.io.ChatContext
import sding.workflow.io.UserInputRequest.*
import sding.workflow.result.*
import sding.workflow.state.ProjectContextState

final class HumanProblemSelectionTask[F[_]: Async](
    chatContext: ChatContext[F]
) extends TaskNode[F]:
  val name = WorkflowStep.HumanProblemSelection

  private def buildSelectionItems(state: ProjectContextState): List[SelectionItem] =
    val matrix   = state.problemsSelected.map(_.decisionMatrix).getOrElse(Nil)
    val problems = state.reformulatedProblems.map(_.problems).getOrElse(Nil)
    matrix.map { psm =>
      val rp      = problems.find(_.problemId == psm.problemId)
      val details = List(
        SelectionDetail("Evidence", psm.evidenceScore.evidenceExplanation),
        SelectionDetail("Complaints", psm.evidenceScore.validationExplanation)
      ) ++ psm.keyInsights.take(3).map(SelectionDetail("Insight", _))
      SelectionItem(
        id = psm.problemId.toString,
        label = rp.map(_.statement).getOrElse("—"),
        description = rp.map(p => s"${p.targetAudience} · ${p.situation}"),
        score = Some(psm.evidenceScore.overallScore),
        tags = List(
          s"Evidence ${psm.evidenceScore.evidenceStrengthScore}/10",
          s"Validation ${psm.evidenceScore.problemValidationScore}/10",
          s"Market ${psm.evidenceScore.marketSignalScore}/10"
        ),
        details = details
      )
    }

  def execute(state: ProjectContextState): F[ProjectContextState] =
    for
      choice <- chatContext.requestSelection("Problem Selection", buildSelectionItems(state), allowRetry = true)
      result <-
        if choice == "retry" then
          for feedback <- chatContext.requestInput(FreeText("What changes would you like?"))
          yield state.appendFeedback("desired_modification", feedback)
        else
          Async[F]
            .fromOption(
              choice.toIntOption,
              sding.domain.AppError.ChatError.InvalidInput("Expected a problem ID number")
            )
            .map(id =>
              state
                .clearFeedback("desired_modification")
                .copy(humanProblemSelectionResult = Some(HumanGateResult(id)))
            )
    yield result

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
