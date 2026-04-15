package chat4s.ai

import cats.effect.Async
import cats.effect.Sync
import chat4s.ai.llm.{AgentConfig, LiveAgent, LiveLlmClient}
import org.typelevel.otel4s.trace.Tracer

object AgentFactory:

  private val DefaultMaxOutputTokens = 4096

  def makeAgent[F[_]: Async: Tracer](
      provider: LlmProvider,
      apiKey: String,
      model: String
  ): F[Agent[F]] = Sync[F].blocking {
    val chatModel = provider match
      case LlmProvider.Gemini =>
        import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel
        GoogleAiGeminiChatModel.builder().apiKey(apiKey).modelName(model)
          .maxOutputTokens(DefaultMaxOutputTokens).build()
      case LlmProvider.OpenAI =>
        import dev.langchain4j.model.openai.OpenAiChatModel
        OpenAiChatModel.builder().apiKey(apiKey).modelName(model)
          .maxTokens(DefaultMaxOutputTokens).build()
      case LlmProvider.Anthropic =>
        import dev.langchain4j.model.anthropic.AnthropicChatModel
        AnthropicChatModel.builder().apiKey(apiKey).modelName(model)
          .maxTokens(DefaultMaxOutputTokens).build()
      case LlmProvider.DeepSeek =>
        import dev.langchain4j.model.openai.OpenAiChatModel
        OpenAiChatModel.builder().baseUrl("https://api.deepseek.com/v1").apiKey(apiKey).modelName(model)
          .maxTokens(DefaultMaxOutputTokens).build()
      case LlmProvider.OpenRouter =>
        import dev.langchain4j.model.openai.OpenAiChatModel
        OpenAiChatModel.builder().baseUrl("https://openrouter.ai/api/v1").apiKey(apiKey).modelName(model)
          .maxTokens(DefaultMaxOutputTokens).build()

    val genAiSystem = provider match
      case LlmProvider.Gemini     => "google"
      case LlmProvider.OpenAI     => "openai"
      case LlmProvider.Anthropic  => "anthropic"
      case LlmProvider.DeepSeek   => "deepseek"
      case LlmProvider.OpenRouter => "openrouter"

    val llmClient = LiveLlmClient.make[F](chatModel, genAiSystem, model)
    val agentConfig = AgentConfig(
      name             = provider.toString.toLowerCase,
      systemPromptName = "default_system",
      modelName        = model,
      temperature      = 1.0,
      thinkingBudget   = None
    )
    val systemPrompt =
      "You are an AI assistant specialized in product brainstorming and design thinking methodologies."
    LiveAgent.make[F](agentConfig, llmClient, systemPrompt)
  }

  def defaultModel(provider: LlmProvider): String = provider match
    case LlmProvider.Gemini     => "gemini-2.0-flash"
    case LlmProvider.OpenAI     => "gpt-4o"
    case LlmProvider.Anthropic  => "claude-sonnet-4-20250514"
    case LlmProvider.DeepSeek   => "deepseek-chat"
    case LlmProvider.OpenRouter => "deepseek/deepseek-v3.2"
