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

final case class WeirdProblem(id: Int, statement: String, fullStatement: String)
    derives Decoder,
      Encoder.AsObject,
      JsonSchemaOf
final case class WeirdProblems(problems: List[WeirdProblem]) derives Decoder, Encoder.AsObject, JsonSchemaOf

final class WeirdProblemGenerationTask[F[_]: Async](
    agent: Agent[F],
    promptLoader: PromptLoader[F],
    chatContext: ChatContext[F]
) extends AgentTaskNode[F](agent, promptLoader, chatContext):
  val name       = WorkflowStep.WeirdProblemGeneration
  val promptName = "WeirdProblemGenerationTask"

  def templateVars(state: ProjectContextState): Map[String, String] =
    Map(
      "project_requirements" -> state.projectRequirements.map(_.toString).getOrElse(""),
      "project_language"     -> state.projectLanguage.getOrElse("English"),
      "noun_list"            -> "cloud, bicycle, mirror, garden, lighthouse"
    )

  def updateState[A](state: ProjectContextState, result: A): ProjectContextState =
    result match
      case wp: WeirdProblems => state.copy(weirdProblems = Some(wp))
      case _                 => state

  def execute(state: ProjectContextState): F[ProjectContextState] =
    runAgent[WeirdProblems](state)
