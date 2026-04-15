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

final case class CompetitiveAnalysisEntry(
    scamperId: Int,
    competitorOfferings: List[String],
    competitiveAdvantages: List[String],
    distributionAdvantage: List[String],
    opportunity: List[String],
    advantageScore: Int
) derives Decoder,
      Encoder.AsObject,
      JsonSchemaOf
final case class CompetitiveAnalysisResult(vpCompetitiveAnalysis: List[CompetitiveAnalysisEntry])
    derives Decoder,
      Encoder.AsObject,
      JsonSchemaOf

final class CompetitiveAnalysisTask[F[_]: Async](
    agent: Agent[F],
    promptLoader: PromptLoader[F],
    chatContext: ChatContext[F],
    searchTool: AgentTool[F]
) extends TaskNode[F]:
  val name = WorkflowStep.CompetitiveAnalysis

  def execute(state: ProjectContextState): F[ProjectContextState] =
    val variants = state.scamperResult.map(_.scamperVariants).getOrElse(Nil)
    for
      _  <- chatContext.sendState(s"Running competitive analysis for ${variants.length} variants...")
      pt <- promptLoader.loadTaskPrompt("CompetitiveAnalysisTask")
      baseVars = Map(
        "empathy_map_result"    -> state.empathyMapResult.map(_.toString).getOrElse(""),
        "project_type"          -> state.projectRequirements.map(_.projectType).getOrElse(""),
        "project_software_type" -> state.projectRequirements.map(_.projectSoftwareType).getOrElse(""),
        "project_language"      -> state.projectLanguage.getOrElse("English")
      )
      promptLink = PromptLink(pt.name, pt.version, chatContext.sessionId)
      entries <- variants.parTraverse { variant =>
        val vars   = baseVars ++ Map("scamper_variant" -> variant.toString)
        val prompt = pt.render(vars)
        agent.tooledCall[CompetitiveAnalysisEntry](prompt, List(searchTool), promptLink).flatMap {
          case AgentResult.Success(v, _) => Async[F].pure(v)
          case AgentResult.Failure(m, _) =>
            Async[F].raiseError(sding.domain.AppError.AgentError.LlmInvocationFailed(name, m))
        }
      }
      (nextState, _) = state.incrementIteration(name)
    yield nextState.copy(competitiveAnalysisResult = Some(CompetitiveAnalysisResult(entries)))
