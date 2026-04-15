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

final case class ReformulatedProblem(
    problemId: Int,
    statement: String,
    targetAudience: String,
    evidenceSnippet: String,
    situation: String,
    impactMetric: String,
    jobToBeDone: String
) derives Decoder,
      Encoder.AsObject,
      JsonSchemaOf
final case class ReformulatedProblems(problems: List[ReformulatedProblem])
    derives Decoder,
      Encoder.AsObject,
      JsonSchemaOf

final class ProblemReformulationTask[F[_]: Async](
    agent: Agent[F],
    promptLoader: PromptLoader[F],
    chatContext: ChatContext[F]
) extends AgentTaskNode[F](agent, promptLoader, chatContext):
  val name       = WorkflowStep.ProblemReformulation
  val promptName = "ProblemReformulationTask"

  def templateVars(state: ProjectContextState): Map[String, String] =
    Map(
      "project_requirements" -> state.projectRequirements.map(_.toString).getOrElse(""),
      "original_problem"     -> state.weirdProblems.map(_.toString).getOrElse(""),
      "empathy_map_result"   -> state.empathyMapResult.map(_.toString).getOrElse(""),
      "project_language"     -> state.projectLanguage.getOrElse("English")
    )

  def updateState[A](state: ProjectContextState, result: A): ProjectContextState =
    result match
      case rp: ReformulatedProblems => state.copy(reformulatedProblems = Some(rp))
      case _                        => state

  def execute(state: ProjectContextState): F[ProjectContextState] =
    runAgent[ReformulatedProblems](state)
