package chat4s.ai.llm

import chat4s.ai.prompt.PromptLink
import dev.langchain4j.model.chat.request.json.JsonObjectSchema

sealed trait LlmMessage
object LlmMessage:
  final case class User(content: String)                                     extends LlmMessage
  final case class AssistantText(content: String)                            extends LlmMessage
  final case class AssistantToolCall(id: String, name: String, args: String) extends LlmMessage
  final case class ToolResult(callId: String, name: String, result: String)  extends LlmMessage

final case class LlmToolSpec(name: String, description: String, schema: Option[JsonObjectSchema] = None)

enum LlmToolResponse:
  case ToolCall(id: String, toolName: String, arguments: String)
  case TextResponse(content: String)

trait LlmClient[F[_]]:
  def chatStructured(
      systemPrompt: String,
      userPrompt: String,
      outputSchema: JsonObjectSchema,
      promptLink: PromptLink
  ): F[String]

  def chatStep(
      systemPrompt: String,
      history: Vector[LlmMessage],
      tools: List[LlmToolSpec],
      promptLink: PromptLink
  ): F[(LlmToolResponse, Vector[LlmMessage])]

  def extractStructured(
      systemPrompt: String,
      history: Vector[LlmMessage],
      outputSchema: JsonObjectSchema,
      promptLink: PromptLink
  ): F[String]
