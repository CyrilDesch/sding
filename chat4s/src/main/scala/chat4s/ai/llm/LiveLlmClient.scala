package chat4s.ai.llm

import cats.effect.Sync
import cats.syntax.all.*
import chat4s.ai.prompt.PromptLink
import dev.langchain4j.agent.tool.ToolExecutionRequest
import dev.langchain4j.agent.tool.ToolSpecification
import dev.langchain4j.data.message.*
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.chat.request.ChatRequest
import dev.langchain4j.model.chat.request.ResponseFormat
import dev.langchain4j.model.chat.request.ResponseFormatType
import dev.langchain4j.model.chat.request.json.JsonObjectSchema
import dev.langchain4j.model.chat.request.json.JsonSchema
import dev.langchain4j.model.chat.response.ChatResponse
import dev.langchain4j.model.output.TokenUsage
import io.circe.Json
import io.circe.syntax.*
import org.typelevel.otel4s.Attribute
import org.typelevel.otel4s.AttributeKey
import org.typelevel.otel4s.trace.Span
import org.typelevel.otel4s.trace.SpanBuilder
import org.typelevel.otel4s.trace.Tracer
import scala.jdk.CollectionConverters.*

object LiveLlmClient:

  def make[F[_]: Sync: Tracer](model: ChatModel, genAiSystem: String, modelName: String): LlmClient[F] =
    new LlmClient[F]:

      private val attrSystem = Attribute(AttributeKey.string("gen_ai.system"), genAiSystem)
      private val attrModel  = Attribute(AttributeKey.string("gen_ai.request.model"), modelName)

      private def withBaseAttrs(builder: SpanBuilder[F], promptLink: PromptLink): SpanBuilder[F] =
        builder
          .addAttribute(attrSystem)
          .addAttribute(attrModel)
          .addAttribute(Attribute(AttributeKey.string("langfuse.observation.prompt.name"), promptLink.name))
          .addAttribute(Attribute(AttributeKey.long("langfuse.observation.prompt.version"), promptLink.version.toLong))

      private def recordUsage(span: Span[F], usage: TokenUsage): F[Unit] =
        Option(usage).fold(Sync[F].unit) { u =>
          span.addAttributes(
            Attribute(AttributeKey.long("gen_ai.usage.input_tokens"), u.inputTokenCount().toLong),
            Attribute(AttributeKey.long("gen_ai.usage.output_tokens"), u.outputTokenCount().toLong)
          )
        }

      private def msgToJson(role: String, content: String): Json =
        Json.obj("role" := role, "content" := content)

      private def historyToInputJson(systemPrompt: String, history: Vector[LlmMessage]): String =
        val system = msgToJson("system", systemPrompt)
        val rest   = history.map {
          case LlmMessage.User(c)                    => msgToJson("user", c)
          case LlmMessage.AssistantText(c)           => msgToJson("assistant", c)
          case LlmMessage.AssistantToolCall(_, n, a) =>
            Json.obj("role" := "assistant", "toolCall" := Json.obj("name" := n, "arguments" := a))
          case LlmMessage.ToolResult(_, n, r) => msgToJson("tool", s"[$n]: $r")
        }
        Json.arr((system +: rest)*).noSpaces

      private def formatAiResponse(response: ChatResponse): String =
        val aiMsg = response.aiMessage()
        if aiMsg.hasToolExecutionRequests() then
          val req = aiMsg.toolExecutionRequests().asScala.head
          Json
            .obj("role" := "assistant", "toolCall" := Json.obj("name" := req.name(), "arguments" := req.arguments()))
            .noSpaces
        else Json.obj("role" := "assistant", "content" := Option(aiMsg.text()).getOrElse("")).noSpaces

      private def buildResponseFormat(schema: JsonObjectSchema): ResponseFormat =
        ResponseFormat.builder()
          .`type`(ResponseFormatType.JSON)
          .jsonSchema(JsonSchema.builder().name("structured_output").rootElement(schema).build())
          .build()

      def chatStructured(
          systemPrompt: String,
          userPrompt: String,
          outputSchema: JsonObjectSchema,
          promptLink: PromptLink
      ): F[String] =
        val messages  = java.util.List.of(SystemMessage.from(systemPrompt), UserMessage.from(userPrompt))
        val request   = ChatRequest.builder()
          .messages(messages)
          .responseFormat(buildResponseFormat(outputSchema))
          .build()
        val inputJson = Json.arr(msgToJson("system", systemPrompt), msgToJson("user", userPrompt)).noSpaces

        withBaseAttrs(Tracer[F].spanBuilder("llm.chatStructured"), promptLink).build.use { span =>
          for
            _        <- span.addAttribute(Attribute(AttributeKey.string("langfuse.observation.input"), inputJson))
            response <- Sync[F].blocking(model.chat(request))
            _        <- recordUsage(span, response.tokenUsage())
            result    = Option(response.aiMessage().text()).getOrElse("{}")
            _        <- span.addAttribute(Attribute(AttributeKey.string("langfuse.observation.output"), result))
          yield result
        }

      def chatStep(
          systemPrompt: String,
          history: Vector[LlmMessage],
          tools: List[LlmToolSpec],
          promptLink: PromptLink
      ): F[(LlmToolResponse, Vector[LlmMessage])] =
        val toolSpecs =
          tools.map { t =>
            val b = ToolSpecification.builder().name(t.name).description(t.description)
            t.schema.foreach(b.parameters)
            b.build()
          }.asJava
        val javaMessages = toJavaMessages(systemPrompt, history)
        val request      = ChatRequest
          .builder()
          .messages(javaMessages)
          .toolSpecifications(toolSpecs)
          .build()
        val inputJson = historyToInputJson(systemPrompt, history)

        withBaseAttrs(Tracer[F].spanBuilder("llm.chatStep"), promptLink).build.use { span =>
          for
            _        <- span.addAttribute(Attribute(AttributeKey.string("langfuse.observation.input"), inputJson))
            response <- Sync[F].blocking(model.chat(request))
            _        <- recordUsage(span, response.tokenUsage())
            outputJson = formatAiResponse(response)
            _ <- span.addAttribute(Attribute(AttributeKey.string("langfuse.observation.output"), outputJson))
            aiMsg = response.aiMessage()
          yield
            if aiMsg.hasToolExecutionRequests() then
              val req     = aiMsg.toolExecutionRequests().asScala.head
              val callId  = Option(req.id()).filter(_.nonEmpty).getOrElse(java.util.UUID.randomUUID().toString)
              val updated = history :+ LlmMessage.AssistantToolCall(callId, req.name(), req.arguments())
              (LlmToolResponse.ToolCall(callId, req.name(), req.arguments()), updated)
            else
              val text    = aiMsg.text()
              val updated = history :+ LlmMessage.AssistantText(text)
              (LlmToolResponse.TextResponse(text), updated)
        }

      def extractStructured(
          systemPrompt: String,
          history: Vector[LlmMessage],
          outputSchema: JsonObjectSchema,
          promptLink: PromptLink
      ): F[String] =
        val request   = ChatRequest.builder()
          .messages(toJavaMessages(systemPrompt, history))
          .responseFormat(buildResponseFormat(outputSchema))
          .build()
        val inputJson = historyToInputJson(systemPrompt, history)

        withBaseAttrs(Tracer[F].spanBuilder("llm.extractStructured"), promptLink).build.use { span =>
          for
            _        <- span.addAttribute(Attribute(AttributeKey.string("langfuse.observation.input"), inputJson))
            response <- Sync[F].blocking(model.chat(request))
            _        <- recordUsage(span, response.tokenUsage())
            result    = Option(response.aiMessage().text()).getOrElse("{}")
            _        <- span.addAttribute(Attribute(AttributeKey.string("langfuse.observation.output"), result))
          yield result
        }

      private def toJavaMessages(
          systemPrompt: String,
          history: Vector[LlmMessage]
      ): java.util.List[ChatMessage] =
        val msgs: Vector[ChatMessage] =
          SystemMessage.from(systemPrompt) +: history.map {
            case LlmMessage.User(content) =>
              UserMessage.from(content)
            case LlmMessage.AssistantText(content) =>
              AiMessage.from(content)
            case LlmMessage.AssistantToolCall(id, name, args) =>
              AiMessage.from(
                java.util.List.of(
                  ToolExecutionRequest.builder().id(id).name(name).arguments(args).build()
                )
              )
            case LlmMessage.ToolResult(callId, name, result) =>
              ToolExecutionResultMessage.from(callId, name, result)
          }
        msgs.asJava
