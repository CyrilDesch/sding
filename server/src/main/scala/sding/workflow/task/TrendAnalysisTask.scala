package sding.workflow.task

import cats.effect.Async
import cats.effect.implicits.*
import cats.syntax.all.*
import chat4s.ai.Agent
import chat4s.ai.AgentResult
import chat4s.ai.AgentTool
import chat4s.ai.JsonSchemaOf
import chat4s.ai.prompt.PromptLink
import chat4s.ai.prompt.PromptLoader
import chat4s.io.ChatContext
import io.circe.Decoder
import io.circe.Encoder
import sding.protocol.WorkflowStep
import sding.workflow.TaskNode
import sding.workflow.state.ProjectContextState

final case class Page(title: String, hasComplaint: Boolean, complaintIntensity: Int, keySnippet: String)
    derives Decoder,
      Encoder.AsObject,
      JsonSchemaOf
final case class ProblemTrend(problemId: Int, pages: List[Page], complaintCoverage: Int, evidenceStrength: String)
    derives Decoder,
      Encoder.AsObject,
      JsonSchemaOf
final case class ProblemsTrends(trends: List[ProblemTrend]) derives Decoder, Encoder.AsObject, JsonSchemaOf

final class TrendAnalysisTask[F[_]: Async](
    agent: Agent[F],
    promptLoader: PromptLoader[F],
    chatContext: ChatContext[F],
    searchTool: AgentTool[F]
) extends TaskNode[F]:
  val name = WorkflowStep.TrendAnalysis

  def execute(state: ProjectContextState): F[ProjectContextState] =
    val problems = state.reformulatedProblems.map(_.problems).getOrElse(Nil)
    for
      _  <- chatContext.sendState(s"Analyzing trends for ${problems.length} problems...")
      pt <- promptLoader.loadTaskPrompt("TrendAnalysisTask")
      baseVars = Map(
        "project_type"          -> state.projectRequirements.map(_.projectType).getOrElse(""),
        "project_software_type" -> state.projectRequirements.map(_.projectSoftwareType).getOrElse(""),
        "project_language"      -> state.projectLanguage.getOrElse("English")
      )
      promptLink = PromptLink(pt.name, pt.version, chatContext.sessionId)
      trends <- problems.zipWithIndex.parTraverse { case (problem, idx) =>
        val vars   = baseVars ++ Map("problem" -> problem.toString)
        val prompt = pt.render(vars)
        Async[F].delay(scribe.info(s"[TrendAnalysis] problem ${idx + 1}/${problems.length}: ${problem.problemId}")) *>
          agent.tooledCall[ProblemTrend](prompt, List(searchTool), promptLink).flatMap {
            case AgentResult.Success(v, _) => Async[F].pure(v.copy(problemId = problem.problemId))
            case AgentResult.Failure(m, _) =>
              Async[F].raiseError(sding.domain.AppError.AgentError.LlmInvocationFailed(name, m))
          }
      }
      (nextState, _) = state.incrementIteration(name)
    yield nextState.copy(problemTrends = Some(ProblemsTrends(trends)))
