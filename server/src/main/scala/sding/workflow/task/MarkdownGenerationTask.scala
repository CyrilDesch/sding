package sding.workflow.task

import cats.effect.Async
import chat4s.ai.JsonSchemaOf
import io.circe.Decoder
import io.circe.Encoder
import sding.protocol.WorkflowStep
import sding.workflow.TaskNode
import sding.workflow.state.ProjectContextState

final case class MarkdownGenerationResult(markdownContent: String, reportTitle: String)
    derives Decoder,
      Encoder.AsObject,
      JsonSchemaOf

final class MarkdownGenerationTask[F[_]: Async]() extends TaskNode[F]:
  val name = WorkflowStep.MarkdownGeneration

  def execute(state: ProjectContextState): F[ProjectContextState] =
    Async[F].pure {
      val report  = state.premiumReportResult.map(_.premiumReport)
      val title   = report.map(_.selectedProjectDetails.projectTitle).getOrElse("Project Report")
      val content = report
        .map(r => s"# ${r.selectedProjectDetails.projectTitle}\n\n## Executive Summary\n\n${r.executiveSummary}")
        .getOrElse("No report data available.")
      state.copy(
        markdownGenerationResult = Some(MarkdownGenerationResult(content, title)),
        isFinalNode = true
      )
    }
