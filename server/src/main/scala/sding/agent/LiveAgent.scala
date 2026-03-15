package sding.agent

import cats.effect.Async
import cats.effect.Sync
import cats.effect.Temporal
import cats.syntax.all.*
import dev.langchain4j.exception.RateLimitException
import dev.langchain4j.model.chat.request.json.JsonObjectSchema
import io.circe.Decoder
import io.circe.parser.decode
import org.typelevel.otel4s.Attribute
import org.typelevel.otel4s.AttributeKey
import org.typelevel.otel4s.trace.Tracer
import scala.concurrent.duration.*

object LiveAgent:

  private val MaxRateLimitRetries   = 3
  private val RateLimitInitialDelay = 30.seconds

  private def isRateLimitException(e: Throwable): Boolean = e match
    case _: RateLimitException   => true
    case _ if e.getCause != null => isRateLimitException(e.getCause)
    case _                       => false

  private def friendlyMessage(e: Throwable): String =
    if isRateLimitException(e) then "AI provider rate limit exceeded — please wait a moment and try again."
    else e.getMessage

  private def sessionAttr(sessionId: String) =
    Attribute(AttributeKey.string("langfuse.session.id"), sessionId)

  def make[F[_]: Async: Tracer](
      config: AgentConfig,
      llmClient: LlmClient[F],
      systemPrompt: String
  ): Agent[F] =
    new Agent[F]:
      val name: String = config.name

      private def withRateLimitRetry[A](label: String, fa: F[A]): F[A] =
        def loop(attempt: Int, delay: FiniteDuration): F[A] =
          Sync[F].delay(scribe.info(s"[$name] $label (attempt ${attempt + 1})")) *>
            fa.handleErrorWith { e =>
              if isRateLimitException(e) && attempt < MaxRateLimitRetries then
                Sync[F].delay(
                  scribe.warn(
                    s"[$name] $label rate-limited — retrying in ${delay.toSeconds}s (attempt ${attempt + 1}/$MaxRateLimitRetries)"
                  )
                ) *> Temporal[F].sleep(delay) *> loop(attempt + 1, delay * 2L)
              else Async[F].raiseError(e)
            }
        loop(0, RateLimitInitialDelay)

      def call[A: Decoder: JsonSchemaOf](prompt: String, promptLink: PromptLink): F[AgentResult[A]] =
        val schema = summon[JsonSchemaOf[A]].element.asInstanceOf[JsonObjectSchema]
        Tracer[F]
          .spanBuilder(promptLink.name)
          .root
          .addAttribute(sessionAttr(promptLink.sessionId))
          .build
          .surround {
            (for
              jsonText <- withRateLimitRetry(
                "call",
                llmClient.chatStructured(systemPrompt, prompt, schema, promptLink)
              )
              result   <- Async[F].fromEither(decode[A](jsonText).leftMap(e => new RuntimeException(e.getMessage)))
            yield AgentResult.Success(result, name): AgentResult[A]).handleError(e =>
              AgentResult.Failure(friendlyMessage(e), name)
            )
          }

      def tooledCall[A: Decoder: JsonSchemaOf](
          prompt: String,
          tools: List[AgentTool[F]],
          maxToolCalls: Int,
          promptLink: PromptLink
      ): F[AgentResult[A]] =
        val toolSpecs = tools.map(t => LlmToolSpec(t.name, t.description))
        val schema    = summon[JsonSchemaOf[A]].element.asInstanceOf[JsonObjectSchema]
        Tracer[F]
          .spanBuilder(promptLink.name)
          .root
          .addAttribute(sessionAttr(promptLink.sessionId))
          .build
          .surround {
            def loop(history: Vector[LlmMessage], remaining: Int, step: Int): F[Vector[LlmMessage]] =
              for
                (toolResponse, nextHistory) <- withRateLimitRetry(
                  s"tooledCall step $step",
                  llmClient.chatStep(systemPrompt, history, toolSpecs, promptLink)
                )
                result <- toolResponse match
                  case LlmToolResponse.TextResponse(_) =>
                    Async[F].pure(nextHistory)
                  case LlmToolResponse.ToolCall(_, _, _) if remaining <= 0 =>
                    Async[F].pure(nextHistory)
                  case LlmToolResponse.ToolCall(id, toolName, arguments) =>
                    for
                      _ <- Sync[F].delay(scribe.debug(s"[$name] tool call: $toolName"))
                      r <- tools.find(_.name == toolName) match
                        case Some(tool) =>
                          tool.execute(arguments).flatMap { output =>
                            loop(nextHistory :+ LlmMessage.ToolResult(id, toolName, output), remaining - 1, step + 1)
                          }
                        case None =>
                          Async[F].raiseError(new RuntimeException(s"Unknown tool: $toolName"))
                    yield r
              yield result

            (for
              history  <- loop(Vector(LlmMessage.User(prompt)), maxToolCalls, 1)
              jsonText <- withRateLimitRetry(
                "extractStructured",
                llmClient.extractStructured(systemPrompt, history, schema, promptLink)
              )
              result <- Async[F].fromEither(decode[A](jsonText).leftMap(e => new RuntimeException(e.getMessage)))
            yield AgentResult.Success(result, name): AgentResult[A]).handleError(e =>
              AgentResult.Failure(friendlyMessage(e), name)
            )
          }
