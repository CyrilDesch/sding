package sding.agent

import cats.effect.Sync
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.chat.request.ChatRequest
import dev.langchain4j.model.chat.request.ResponseFormat
import dev.langchain4j.model.chat.request.ResponseFormatType
import scala.jdk.CollectionConverters.*

object LiveLlmClient:
  def make[F[_]: Sync](model: ChatModel): LlmClient[F] =
    new LlmClient[F]:
      def chat(systemPrompt: String, userPrompt: String, jsonMode: Boolean): F[String] =
        Sync[F].blocking {
          val messages = java.util.List.of(
            SystemMessage.from(systemPrompt),
            UserMessage.from(userPrompt)
          )
          val builder = ChatRequest.builder().messages(messages)
          if jsonMode then builder.responseFormat(ResponseFormat.builder().`type`(ResponseFormatType.JSON).build())
          val response = model.chat(builder.build())
          response.aiMessage().text()
        }

      def chatWithTools(
          systemPrompt: String,
          userPrompt: String,
          tools: List[LlmToolSpec]
      ): F[LlmToolResponse] =
        Sync[F].blocking {
          val messages = java.util.List.of(
            SystemMessage.from(systemPrompt),
            UserMessage.from(userPrompt)
          )
          val response = model.chat(ChatRequest.builder().messages(messages).build())
          val aiMsg    = response.aiMessage()
          if aiMsg.hasToolExecutionRequests then
            val req = aiMsg.toolExecutionRequests().asScala.head
            LlmToolResponse.ToolCall(req.name(), req.arguments())
          else LlmToolResponse.TextResponse(aiMsg.text())
        }
