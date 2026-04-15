package sding.workflow

import cats.effect.Async
import chat4s.ai.Agent
import chat4s.ai.AgentStep
import chat4s.ai.prompt.PromptLoader
import chat4s.io.ChatContext
import sding.domain.AppError
import sding.workflow.state.ProjectContextState

abstract class AgentTaskNode[F[_]: Async](
    agent: Agent[F],
    promptLoader: PromptLoader[F],
    chatContext: ChatContext[F]
) extends AgentStep[F, ProjectContextState](agent, promptLoader, chatContext)
    with TaskNode[F]:

  override protected def displayName: String               = name.friendlyName
  override protected def onFailure(msg: String): Throwable = AppError.AgentError.LlmInvocationFailed(name, msg)
  override protected def beforeRun(state: ProjectContextState): ProjectContextState =
    state.incrementIteration(name)._1
