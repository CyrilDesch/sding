package sding.workflow.task

import cats.effect.Async
import cats.effect.implicits.*
import cats.syntax.all.*
import chat4s.ai.Agent
import chat4s.ai.AgentResult
import chat4s.ai.JsonSchemaOf
import chat4s.ai.prompt.PromptLink
import chat4s.ai.prompt.PromptLoader
import chat4s.io.ChatContext
import io.circe.Decoder
import io.circe.Encoder
import sding.protocol.WorkflowStep
import sding.workflow.TaskNode
import sding.workflow.state.ProjectContextState

final case class Interview(
    keyQuotes: List[String],
    painSeverity: Int,
    currentSolutions: List[String],
    willingnessToPay: Int
) derives Decoder,
      Encoder.AsObject,
      JsonSchemaOf
final case class UserInterviewResult(interviews: List[Interview]) derives Decoder, Encoder.AsObject, JsonSchemaOf

final class UserInterviewsTask[F[_]: Async](
    agent: Agent[F],
    promptLoader: PromptLoader[F],
    chatContext: ChatContext[F]
) extends TaskNode[F]:
  val name = WorkflowStep.UserInterviews

  def execute(state: ProjectContextState): F[ProjectContextState] =
    for
      _  <- chatContext.sendState("Running parallel user interviews...")
      pt <- promptLoader.loadTaskPrompt("UserInterviewTask")
      baseVars = Map(
        "project_requirements"  -> state.projectRequirements.map(_.toString).getOrElse(""),
        "reformulated_problems" -> state.reformulatedProblems.map(_.toString).getOrElse(""),
        "project_language"      -> state.projectLanguage.getOrElse("English")
      )
      promptLink    = PromptLink(pt.name, pt.version, chatContext.sessionId)
      hyperPrompt   = pt.render(baseVars ++ Map("persona" -> "hyper_concerned_user"))
      skepticPrompt = pt.render(baseVars ++ Map("persona" -> "skeptical_user"))
      (hyperResult, skepticResult) <- (
        agent.call[Interview](hyperPrompt, promptLink),
        agent.call[Interview](skepticPrompt, promptLink)
      ).parTupled
      interviews <- (hyperResult, skepticResult) match
        case (AgentResult.Success(h, _), AgentResult.Success(s, _)) =>
          Async[F].pure(UserInterviewResult(List(h, s)))
        case (AgentResult.Failure(msg, _), _) =>
          Async[F].raiseError(sding.domain.AppError.AgentError.LlmInvocationFailed(name, msg))
        case (_, AgentResult.Failure(msg, _)) =>
          Async[F].raiseError(sding.domain.AppError.AgentError.LlmInvocationFailed(name, msg))
      (nextState, _) = state.incrementIteration(name)
    yield nextState.copy(userInterviews = Some(interviews))
