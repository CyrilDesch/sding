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

final case class JobToBeDone(
    id: Int,
    jobStatement: String,
    importance: Int,
    satisfactionToday: Int,
    archetypeLabel: String
) derives Decoder,
      Encoder.AsObject,
      JsonSchemaOf
final case class JTBDDefinitionResult(jobs: List[JobToBeDone]) derives Decoder, Encoder.AsObject, JsonSchemaOf

final class JTBDDefinitionTask[F[_]: Async](
    agent: Agent[F],
    promptLoader: PromptLoader[F],
    chatContext: ChatContext[F]
) extends AgentTaskNode[F](agent, promptLoader, chatContext):
  val name       = WorkflowStep.JtbdDefinition
  val promptName = "JTBDDefinitionTask"

  def templateVars(state: ProjectContextState): Map[String, String] =
    Map(
      "project_requirements" -> state.projectRequirements.map(_.toString).getOrElse(""),
      "empathy_map_result"   -> state.empathyMapResult.map(_.toString).getOrElse(""),
      "project_language"     -> state.projectLanguage.getOrElse("English")
    )

  def updateState[A](state: ProjectContextState, result: A): ProjectContextState =
    result match
      case jd: JTBDDefinitionResult => state.copy(jtbdDefinitionResult = Some(jd))
      case _                        => state

  def execute(state: ProjectContextState): F[ProjectContextState] =
    runAgent[JTBDDefinitionResult](state)
