package sding.agent

import io.circe.Decoder

trait Agent[F[_]]:
  def name: String
  def call[A: Decoder: JsonSchemaOf](prompt: String, promptLink: PromptLink): F[AgentResult[A]]
  def tooledCall[A: Decoder: JsonSchemaOf](
      prompt: String,
      tools: List[AgentTool[F]],
      maxToolCalls: Int,
      promptLink: PromptLink
  ): F[AgentResult[A]]
