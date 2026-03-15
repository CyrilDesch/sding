package sding.agent

import cats.effect.Async
import cats.effect.Sync
import sding.protocol.LlmProvider

object AgentFactory:

  def makeAgent[F[_]: Async](
      provider: LlmProvider,
      apiKey: String,
      model: String,
      quotaManager: QuotaManager[F]
  ): F[Agent[F]] = Sync[F].blocking {
    val chatModel = provider match
      case LlmProvider.Gemini =>
        import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel
        GoogleAiGeminiChatModel.builder().apiKey(apiKey).modelName(model).build()
      case LlmProvider.OpenAI =>
        import dev.langchain4j.model.openai.OpenAiChatModel
        OpenAiChatModel.builder().apiKey(apiKey).modelName(model).build()
      case LlmProvider.Anthropic =>
        import dev.langchain4j.model.anthropic.AnthropicChatModel
        AnthropicChatModel.builder().apiKey(apiKey).modelName(model).build()

    val llmClient    = LiveLlmClient.make[F](chatModel)
    val agentConfig  = AgentConfig(provider.toString.toLowerCase, "default_system", model, 1.0, None)
    val systemPrompt =
      "You are an AI assistant specialized in product brainstorming and design thinking methodologies."
    LiveAgent.make[F](agentConfig, llmClient, systemPrompt, quotaManager)
  }

  def defaultModel(provider: LlmProvider): String = provider match
    case LlmProvider.Gemini    => "gemini-2.0-flash"
    case LlmProvider.OpenAI    => "gpt-4o"
    case LlmProvider.Anthropic => "claude-sonnet-4-20250514"
