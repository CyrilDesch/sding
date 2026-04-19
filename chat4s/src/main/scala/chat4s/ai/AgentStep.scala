package chat4s.ai

import cats.effect.Async
import cats.syntax.all.*
import chat4s.ai.prompt.PromptLink
import chat4s.ai.prompt.PromptLoader
import chat4s.graph.Step
import chat4s.io.ChatContext
import io.circe.Decoder

/** Base class for workflow steps that delegate to an LLM [[Agent]].
  *
  * Subclasses must implement:
  *  - [[promptName]]: the template to load from [[PromptLoader]]
  *  - [[templateVars]]: variables to interpolate into the template
  *  - [[updateState]]: how to fold the agent result back into state
  */
abstract class AgentStep[F[_]: Async, S](
    agent: Agent[F],
    promptLoader: PromptLoader[F],
    chatContext: ChatContext[F]
) extends Step[F, S]:

  protected def promptName: String
  protected def templateVars(state: S): Map[String, String]
  protected def updateState[A](state: S, result: A): S
  protected def displayName: String               = id
  protected def onFailure(msg: String): Throwable = new RuntimeException(s"LLM invocation failed for step '$id': $msg")
  protected def beforeRun(state: S): S            = state

  protected def runAgent[A: Decoder: JsonSchemaOf](state: S): F[S] =
    val preparedState = beforeRun(state)
    for
      _  <- chatContext.sendState(s"Running $displayName...")
      pt <- promptLoader.loadTaskPrompt(promptName)
      vars            = templateVars(preparedState)
      formattedPrompt = pt.render(vars)
      promptLink      = PromptLink(pt.name, pt.version, chatContext.sessionId)
      agentResult <- agent.call[A](formattedPrompt, promptLink)
      result      <- agentResult match
        case AgentResult.Success(value, _) =>
          Async[F].pure(updateState(preparedState, value))
        case AgentResult.Failure(msg, _) =>
          Async[F].raiseError(onFailure(msg))
    yield result
