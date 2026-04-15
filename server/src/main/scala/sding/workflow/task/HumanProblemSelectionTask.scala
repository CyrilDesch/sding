package sding.workflow.task

import cats.effect.Async
import cats.syntax.all.*
import chat4s.ai.JsonSchemaOf
import chat4s.io.ChatContext
import chat4s.io.SelectionDetail
import chat4s.io.SelectionItem
import chat4s.io.UserInputRequest.*
import io.circe.Decoder
import io.circe.Encoder
import sding.protocol.WorkflowStep
import sding.workflow.TaskNode
import sding.workflow.state.ProjectContextState

final case class HumanGateResult(selectedProblemId: Int) derives Decoder, Encoder.AsObject, JsonSchemaOf

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
