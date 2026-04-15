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

final case class SketchVariant(hmwId: Int, sketchDescriptions: List[String])
    derives Decoder,
      Encoder.AsObject,
      JsonSchemaOf
final case class Crazy8sResult(variants: List[SketchVariant]) derives Decoder, Encoder.AsObject, JsonSchemaOf

final class Crazy8sTask[F[_]: Async](
    agent: Agent[F],
    promptLoader: PromptLoader[F],
    chatContext: ChatContext[F]
) extends AgentTaskNode[F](agent, promptLoader, chatContext):
  val name       = WorkflowStep.Crazy8s
  val promptName = "Crazy8sTask"

  def templateVars(state: ProjectContextState): Map[String, String] =
    Map(
      "project_requirements" -> state.projectRequirements.map(_.toString).getOrElse(""),
      "empathy_map_result"   -> state.empathyMapResult.map(_.toString).getOrElse(""),
      "project_language"     -> state.projectLanguage.getOrElse("English")
    )

  def updateState[A](state: ProjectContextState, result: A): ProjectContextState =
    result match
      case c8: Crazy8sResult => state.copy(crazy8sResult = Some(c8))
      case _                 => state

  def execute(state: ProjectContextState): F[ProjectContextState] =
    runAgent[Crazy8sResult](state)
