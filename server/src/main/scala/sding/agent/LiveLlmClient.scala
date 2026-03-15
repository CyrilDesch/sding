package sding.agent

import cats.effect.Sync
import dev.langchain4j.agent.tool.ToolExecutionRequest
import dev.langchain4j.agent.tool.ToolSpecification
import dev.langchain4j.data.message.*
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.chat.request.ChatRequest
import dev.langchain4j.model.chat.request.ToolChoice
import dev.langchain4j.model.chat.request.json.JsonObjectSchema
import org.typelevel.otel4s.trace.Tracer
import scala.jdk.CollectionConverters.*

object LiveLlmClient:
  private val OutputToolName = "structured_output"

  def make[F[_]: Sync: Tracer](model: ChatModel): LlmClient[F] =
    new LlmClient[F]:

      def chatStructured(
          systemPrompt: String,
          userPrompt: String,
          outputSchema: JsonObjectSchema
      ): F[String] =
        Tracer[F].span("llm.chatStructured").surround {
          Sync[F].blocking {
            val messages = java.util.List.of(
              SystemMessage.from(systemPrompt),
              UserMessage.from(userPrompt)
            )
            val request = ChatRequest
              .builder()
              .messages(messages)
              .toolSpecifications(buildOutputTool(outputSchema))
              .toolChoice(ToolChoice.REQUIRED)
              .build()
            extractToolArguments(model.chat(request))
          }
        }

      def chatStep(
          systemPrompt: String,
          history: Vector[LlmMessage],
          tools: List[LlmToolSpec]
      ): F[(LlmToolResponse, Vector[LlmMessage])] =
        Tracer[F].span("llm.chatStep").surround {
          Sync[F].blocking {
            val toolSpecs =
              tools.map(t => ToolSpecification.builder().name(t.name).description(t.description).build()).asJava
            val request = ChatRequest
              .builder()
              .messages(toJavaMessages(systemPrompt, history))
              .toolSpecifications(toolSpecs)
              .build()
            val aiMsg = model.chat(request).aiMessage()
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
        }

      def extractStructured(
          systemPrompt: String,
          history: Vector[LlmMessage],
          outputSchema: JsonObjectSchema
      ): F[String] =
        Tracer[F].span("llm.extractStructured").surround {
          Sync[F].blocking {
            val instruction = "Now call the structured_output tool with your complete findings."
            val fullHistory = history :+ LlmMessage.User(instruction)
            val request     = ChatRequest
              .builder()
              .messages(toJavaMessages(systemPrompt, fullHistory))
              .toolSpecifications(buildOutputTool(outputSchema))
              .toolChoice(ToolChoice.REQUIRED)
              .build()
            extractToolArguments(model.chat(request))
          }
        }

      private def buildOutputTool(schema: JsonObjectSchema): ToolSpecification =
        ToolSpecification
          .builder()
          .name(OutputToolName)
          .description("Return the complete structured result.")
          .parameters(schema)
          .build()

      private def extractToolArguments(response: dev.langchain4j.model.chat.response.ChatResponse): String =
        val aiMsg = response.aiMessage()
        if aiMsg.hasToolExecutionRequests() then aiMsg.toolExecutionRequests().asScala.head.arguments()
        else Option(aiMsg.text()).getOrElse("{}")

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
