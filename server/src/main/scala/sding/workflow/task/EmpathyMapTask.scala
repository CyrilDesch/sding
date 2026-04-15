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

final case class EmpathyMap(
    personaDescription: String,
    see: List[String],
    hear: List[String],
    think: List[String],
    feel: List[String],
    pains: List[String],
    desiredOutcomes: List[String],
    insights: List[String]
) derives Decoder,
      Encoder.AsObject,
      JsonSchemaOf
final case class EmpathyMapResult(empathyMap: EmpathyMap) derives Decoder, Encoder.AsObject, JsonSchemaOf

final class EmpathyMapTask[F[_]: Async](
    agent: Agent[F],
    promptLoader: PromptLoader[F],
    chatContext: ChatContext[F]
) extends AgentTaskNode[F](agent, promptLoader, chatContext):
  val name       = WorkflowStep.EmpathyMap
  val promptName = "EmpathyMapTask"

  def templateVars(state: ProjectContextState): Map[String, String] =
    Map(
      "project_requirements" -> state.projectRequirements.map(_.toString).getOrElse(""),
      "project_language"     -> state.projectLanguage.getOrElse("English")
    )

  def updateState[A](state: ProjectContextState, result: A): ProjectContextState =
    result match
      case em: EmpathyMapResult => state.copy(empathyMapResult = Some(em))
      case _                    => state

  def execute(state: ProjectContextState): F[ProjectContextState] =
    runAgent[EmpathyMapResult](state)
