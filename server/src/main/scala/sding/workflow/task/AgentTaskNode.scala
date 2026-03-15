package sding.workflow.task

import cats.effect.Async
import cats.syntax.all.*
import io.circe.Decoder
import sding.agent.Agent
import sding.agent.AgentResult
import sding.agent.JsonSchemaOf
import sding.agent.PromptLoader
import sding.workflow.io.ChatContext
import sding.workflow.state.ProjectContextState

abstract class AgentTaskNode[F[_]: Async](
    agent: Agent[F],
    promptLoader: PromptLoader[F],
    chatContext: ChatContext[F]
) extends TaskNode[F]:

  protected def promptName: String
  protected def templateVars(state: ProjectContextState): Map[String, String]
  protected def updateState[A](state: ProjectContextState, result: A): ProjectContextState

  protected def runAgent[A: Decoder: JsonSchemaOf](state: ProjectContextState): F[ProjectContextState] =
    for
      _  <- chatContext.sendState(s"Running ${name.friendlyName}...")
      pt <- promptLoader.loadTaskPrompt(promptName)
      vars            = templateVars(state)
      formattedPrompt = pt.render(vars)
      promptLink      = sding.agent.PromptLink(pt.name, pt.version, chatContext.sessionId)
      (nextState, _)  = state.incrementIteration(name)
      agentResult <- agent.call[A](formattedPrompt, promptLink)
      result      <- agentResult match
        case AgentResult.Success(value, _) =>
          Async[F].pure(updateState(nextState, value))
        case AgentResult.Failure(msg, _) =>
          Async[F].raiseError(
            sding.domain.AppError.AgentError.LlmInvocationFailed(name, msg)
          )
    yield result
