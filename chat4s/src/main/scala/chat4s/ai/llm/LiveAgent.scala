package chat4s.ai.llm

import cats.effect.Async
import cats.effect.Sync
import cats.effect.Temporal
import cats.syntax.all.*
import chat4s.ai.Agent
import chat4s.ai.AgentResult
import chat4s.ai.AgentTool
import chat4s.ai.JsonSchemaOf
import chat4s.ai.SchemaElement
import chat4s.ai.prompt.PromptLink
import dev.langchain4j.exception.RateLimitException
import dev.langchain4j.model.chat.request.json as lc4j
import io.circe.Decoder
import io.circe.Json
import io.circe.parser.decode
import io.circe.syntax.*
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

  /** Convert a [[SchemaElement]] to a langchain4j JSON schema element. */
  private def toLC4j(e: SchemaElement): lc4j.JsonSchemaElement = e match
    case SchemaElement.JsString       => lc4j.JsonStringSchema.builder().build()
    case SchemaElement.JsInteger      => lc4j.JsonIntegerSchema.builder().build()
    case SchemaElement.JsNumber       => lc4j.JsonNumberSchema.builder().build()
    case SchemaElement.JsBoolean      => lc4j.JsonBooleanSchema.builder().build()
    case SchemaElement.JsArray(items) =>
      lc4j.JsonArraySchema.builder().items(toLC4j(items)).build()
    case SchemaElement.JsObject(props, required) =>
      val builder = lc4j.JsonObjectSchema.builder()
      props.foreach { (name, schema) => builder.addProperty(name, toLC4j(schema)) }
      if required.nonEmpty then builder.required(required*)
      builder.build()

  private def toObjectSchema(e: SchemaElement): lc4j.JsonObjectSchema =
    toLC4j(e).asInstanceOf[lc4j.JsonObjectSchema]

  def make[F[_]: Async: Tracer](
      agentConfig: AgentConfig,
      llmClient: LlmClient[F],
      systemPrompt: String
  ): Agent[F] =
    new Agent[F]:
      val name: String = agentConfig.name

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
        val schema    = toObjectSchema(summon[JsonSchemaOf[A]].element)
        val inputJson = Json.arr(Json.obj("role" := "user", "content" := prompt)).noSpaces
        Tracer[F]
          .spanBuilder(promptLink.name)
          .root
          .addAttribute(sessionAttr(promptLink.sessionId))
          .build
          .use { span =>
            (for
              _        <- span.addAttribute(Attribute(AttributeKey.string("langfuse.observation.input"), inputJson))
              jsonText <- withRateLimitRetry(
                "call",
                llmClient.chatStructured(systemPrompt, prompt, schema, promptLink)
              )
              _      <- span.addAttribute(Attribute(AttributeKey.string("langfuse.observation.output"), jsonText))
              result <- Async[F].fromEither(decode[A](jsonText).leftMap(e => new RuntimeException(e.getMessage)))
            yield AgentResult.Success(result, name): AgentResult[A])
              .handleError(e => AgentResult.Failure(friendlyMessage(e), name))
          }

      def tooledCall[A: Decoder: JsonSchemaOf](
          prompt: String,
          tools: List[AgentTool[F]],
          promptLink: PromptLink,
          config: chat4s.ai.CallConfig = chat4s.ai.CallConfig.default
      ): F[AgentResult[A]] =
        val schema         = toObjectSchema(summon[JsonSchemaOf[A]].element)
        val outputToolName = "structured_output"
        val outputToolSpec = LlmToolSpec(outputToolName, "Return the complete structured result.", Some(schema))
        val baseToolSpecs  = tools.map(t => LlmToolSpec(t.name, t.description, t.inputSchema.map(toObjectSchema)))
        val allToolSpecs   = baseToolSpecs :+ outputToolSpec
        val inputJson      = Json.arr(Json.obj("role" := "user", "content" := prompt)).noSpaces
        Tracer[F]
          .spanBuilder(promptLink.name)
          .root
          .addAttribute(sessionAttr(promptLink.sessionId))
          .build
          .use { span =>
            def loop(history: Vector[LlmMessage], remaining: Int, step: Int): F[String] =
              val currentTools = if remaining <= 1 then allToolSpecs else baseToolSpecs
              for
                (toolResponse, nextHistory) <- withRateLimitRetry(
                  s"tooledCall step $step",
                  llmClient.chatStep(systemPrompt, history, currentTools, promptLink)
                )
                result <- toolResponse match
                  case LlmToolResponse.ToolCall(_, toolName, arguments) if toolName == outputToolName =>
                    Async[F].pure(arguments)
                  case LlmToolResponse.TextResponse(_) =>
                    withRateLimitRetry(
                      "extractStructured",
                      llmClient.extractStructured(systemPrompt, nextHistory, schema, promptLink)
                    )
                  case LlmToolResponse.ToolCall(_, _, _) if remaining <= 0 =>
                    withRateLimitRetry(
                      "extractStructured",
                      llmClient.extractStructured(systemPrompt, nextHistory, schema, promptLink)
                    )
                  case LlmToolResponse.ToolCall(id, toolName, arguments) =>
                    for
                      _ <- Sync[F].delay(scribe.debug(s"[$name] tool call: $toolName"))
                      r <- tools.find(_.name == toolName) match
                        case Some(tool) =>
                          tool.execute(arguments).flatMap { output =>
                            val truncated =
                              if output.length > config.maxToolResultChars
                              then output.take(config.maxToolResultChars) + "…"
                              else output
                            loop(nextHistory :+ LlmMessage.ToolResult(id, toolName, truncated), remaining - 1, step + 1)
                          }
                        case None =>
                          Async[F].raiseError(new RuntimeException(s"Unknown tool: $toolName"))
                    yield r
              yield result

            (for
              _        <- span.addAttribute(Attribute(AttributeKey.string("langfuse.observation.input"), inputJson))
              jsonText <- loop(Vector(LlmMessage.User(prompt)), config.maxToolCallRounds, 1)
              _        <- span.addAttribute(Attribute(AttributeKey.string("langfuse.observation.output"), jsonText))
              result   <- Async[F].fromEither(decode[A](jsonText).leftMap(e => new RuntimeException(e.getMessage)))
            yield AgentResult.Success(result, name): AgentResult[A])
              .handleError(e => AgentResult.Failure(friendlyMessage(e), name))
          }
