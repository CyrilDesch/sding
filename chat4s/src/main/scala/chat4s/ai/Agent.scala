package chat4s.ai

import chat4s.ai.prompt.PromptLink
import io.circe.Decoder

trait Agent[F[_]]:
  def name: String
  def call[A: Decoder: JsonSchemaOf](prompt: String, promptLink: PromptLink): F[AgentResult[A]]
  def tooledCall[A: Decoder: JsonSchemaOf](
      prompt: String,
      tools: List[AgentTool[F]],
      promptLink: PromptLink,
      config: CallConfig = CallConfig.default
  ): F[AgentResult[A]]
