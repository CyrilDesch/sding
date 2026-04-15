package sding.workflow.task

import cats.effect.Async
import chat4s.ai.Agent
import chat4s.ai.JsonSchemaOf
import chat4s.ai.prompt.PromptLoader
import chat4s.io.ChatContext
import io.circe.Decoder
import io.circe.Encoder
import sding.protocol.WorkflowStep
import sding.workflow.AgentTaskNode
import sding.workflow.state.ProjectContextState

final case class HMWQuestion(
    id: Int,
    question: String,
    impact: Int,
    effort: Int,
    viralLoopStrength: Int,
    jobsId: Int
) derives Decoder,
      Encoder.AsObject,
      JsonSchemaOf
final case class HMWTopResult(topQuestions: List[HMWQuestion]) derives Decoder, Encoder.AsObject, JsonSchemaOf

final class HMWTask[F[_]: Async](
    agent: Agent[F],
    promptLoader: PromptLoader[F],
    chatContext: ChatContext[F]
) extends AgentTaskNode[F](agent, promptLoader, chatContext):
  val name       = WorkflowStep.Hmw
  val promptName = "HMWTask"

  def templateVars(state: ProjectContextState): Map[String, String] =
    Map(
      "project_requirements" -> state.projectRequirements.map(_.toString).getOrElse(""),
      "empathy_map_result"   -> state.empathyMapResult.map(_.toString).getOrElse(""),
      "jtbd_result"          -> state.jtbdDefinitionResult.map(_.toString).getOrElse(""),
      "project_language"     -> state.projectLanguage.getOrElse("English")
    )

  def updateState[A](state: ProjectContextState, result: A): ProjectContextState =
    result match
      case hmw: HMWTopResult => state.copy(hmwTopResult = Some(hmw))
      case _                 => state

  def execute(state: ProjectContextState): F[ProjectContextState] =
    runAgent[HMWTopResult](state)
