package sding.agent

trait LlmClient[F[_]]:
  def chat(systemPrompt: String, userPrompt: String, jsonMode: Boolean): F[String]
  def chatWithTools(
      systemPrompt: String,
      userPrompt: String,
      tools: List[LlmToolSpec]
  ): F[LlmToolResponse]

final case class LlmToolSpec(name: String, description: String, parametersJson: String)

enum LlmToolResponse:
  case ToolCall(toolName: String, arguments: String)
  case TextResponse(content: String)
