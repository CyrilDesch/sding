package sding.workflow.task

import cats.effect.Async
import cats.syntax.all.*
import chat4s.ai.JsonSchemaOf
import chat4s.io.ChatContext
import io.circe.Decoder
import io.circe.Encoder
import sding.protocol.WorkflowStep
import sding.workflow.TaskNode
import sding.workflow.state.ProjectContextState

final case class EvidenceScore(
    evidenceStrengthScore: Int,
    problemValidationScore: Int,
    marketSignalScore: Int,
    overallScore: Double,
    evidenceExplanation: String,
    validationExplanation: String,
    marketExplanation: String,
    decisionInsight: String
) derives Decoder,
      Encoder.AsObject,
      JsonSchemaOf
final case class ProblemScoreMatrix(
    problemId: Int,
    evidenceScore: EvidenceScore,
    totalPagesFound: Int,
    pagesWithComplaints: Int,
    highestComplaintIntensity: Int,
    evidenceStrength: String,
    keyInsights: List[String]
) derives Decoder,
      Encoder.AsObject,
      JsonSchemaOf
final case class ProblemsSelectedResult(
    decisionMatrix: List[ProblemScoreMatrix],
    selectionGuidance: Option[String] = None
) derives Decoder,
      Encoder.AsObject,
      JsonSchemaOf

final class ProblemSelectionTask[F[_]: Async](
    chatContext: ChatContext[F]
) extends TaskNode[F]:
  val name = WorkflowStep.ProblemSelection

  def execute(state: ProjectContextState): F[ProjectContextState] =
    chatContext.sendState("Scoring and ranking problems...") *> Async[F].delay {
      val trends   = state.problemTrends.map(_.trends).getOrElse(Nil)
      val problems = state.reformulatedProblems.map(_.problems).getOrElse(Nil)

      val matrix = trends.map { trend =>
        val totalPages          = trend.pages.length
        val pagesWithComplaints = trend.pages.count(_.hasComplaint)
        val maxIntensity        = if trend.pages.isEmpty then 0 else trend.pages.map(_.complaintIntensity).max

        val evidenceStrengthScore  = Math.min(10, totalPages * 2)
        val problemValidationScore = if pagesWithComplaints > 0 then Math.min(10, pagesWithComplaints * 3) else 1
        val marketSignalScore      = Math.min(10, maxIntensity * 2)
        val overall                = (evidenceStrengthScore + problemValidationScore + marketSignalScore) / 3.0

        val matchingProblem = problems.find(_.problemId == trend.problemId)
        val insights        = trend.pages.filter(_.hasComplaint).map(_.keySnippet).take(3)

        ProblemScoreMatrix(
          problemId = trend.problemId,
          evidenceScore = EvidenceScore(
            evidenceStrengthScore = evidenceStrengthScore,
            problemValidationScore = problemValidationScore,
            marketSignalScore = marketSignalScore,
            overallScore = overall,
            evidenceExplanation = s"Found $totalPages pages with relevant content",
            validationExplanation = s"$pagesWithComplaints pages contain user complaints",
            marketExplanation = s"Highest complaint intensity: $maxIntensity",
            decisionInsight = matchingProblem.map(_.statement).getOrElse("N/A")
          ),
          totalPagesFound = totalPages,
          pagesWithComplaints = pagesWithComplaints,
          highestComplaintIntensity = maxIntensity,
          evidenceStrength = trend.evidenceStrength,
          keyInsights = insights
        )
      }

      val sorted         = matrix.sortBy(-_.evidenceScore.overallScore)
      val guidance       = if sorted.nonEmpty then Some(s"Top problem: ID ${sorted.head.problemId}") else None
      val (nextState, _) = state.incrementIteration(name)
      nextState.copy(problemsSelected = Some(ProblemsSelectedResult(sorted, guidance)))
    }
