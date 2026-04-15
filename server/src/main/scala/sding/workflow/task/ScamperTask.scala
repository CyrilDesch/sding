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

final case class ScamperVariant(
    id: Int,
    hmwId: Int,
    substitute: String,
    combine: String,
    adapt: String,
    modify: String,
    putToAnotherUse: String,
    eliminate: String,
    reverse: String,
    feasibilityScore: Int
) derives Decoder,
      Encoder.AsObject,
      JsonSchemaOf
final case class ScamperResult(scamperVariants: List[ScamperVariant]) derives Decoder, Encoder.AsObject, JsonSchemaOf

final class ScamperTask[F[_]: Async](
    agent: Agent[F],
    promptLoader: PromptLoader[F],
    chatContext: ChatContext[F]
) extends AgentTaskNode[F](agent, promptLoader, chatContext):
  val name       = WorkflowStep.Scamper
  val promptName = "ScamperTask"

  def templateVars(state: ProjectContextState): Map[String, String] =
    Map(
      "project_requirements" -> state.projectRequirements.map(_.toString).getOrElse(""),
      "empathy_map_result"   -> state.empathyMapResult.map(_.toString).getOrElse(""),
      "weird_problems"       -> state.weirdProblems.map(_.toString).getOrElse(""),
      "project_language"     -> state.projectLanguage.getOrElse("English")
    )

  def updateState[A](state: ProjectContextState, result: A): ProjectContextState =
    result match
      case sc: ScamperResult => state.copy(scamperResult = Some(sc))
      case _                 => state

  def execute(state: ProjectContextState): F[ProjectContextState] =
    runAgent[ScamperResult](state)
