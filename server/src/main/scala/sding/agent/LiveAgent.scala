package sding.agent

import cats.effect.Async
import cats.syntax.all.*
import io.circe.Decoder
import io.circe.parser.decode

object LiveAgent:
  def make[F[_]: Async](
      config: AgentConfig,
      llmClient: LlmClient[F],
      systemPrompt: String,
      quotaManager: QuotaManager[F]
  ): Agent[F] =
    new Agent[F]:
      val name: String = config.name

      def call[A: Decoder](prompt: String): F[AgentResult[A]] =
        (for
          _        <- quotaManager.acquireSlot
          jsonText <- llmClient.chat(systemPrompt, prompt, jsonMode = true)
          result   <- Async[F].fromEither(decode[A](jsonText).leftMap(e => new RuntimeException(e.getMessage)))
        yield AgentResult.Success(result, name): AgentResult[A])
          .handleError(e => AgentResult.Failure(e.getMessage, name))

      def tooledCall[A: Decoder](
          prompt: String,
          tools: List[AgentTool[F]],
          maxToolCalls: Int
      ): F[AgentResult[A]] =
        val toolSpecs = tools.map(t => LlmToolSpec(t.name, t.description, "{}"))

        def loop(currentPrompt: String, remaining: Int): F[String] =
          for
            _        <- quotaManager.acquireSlot
            response <- llmClient.chatWithTools(systemPrompt, currentPrompt, toolSpecs)
            result   <- response match
              case LlmToolResponse.TextResponse(text) =>
                Async[F].pure(text)
              case LlmToolResponse.ToolCall(toolName, arguments) if remaining <= 0 =>
                llmClient.chat(systemPrompt, s"Tool $toolName returned: $arguments\n\n$prompt", jsonMode = true)
              case LlmToolResponse.ToolCall(toolName, arguments) =>
                tools.find(_.name == toolName) match
                  case Some(tool) =>
                    tool
                      .execute(arguments)
                      .flatMap(output =>
                        loop(s"Tool $toolName returned: $output\n\nOriginal request: $prompt", remaining - 1)
                      )
                  case None =>
                    Async[F].raiseError(new RuntimeException(s"Unknown tool: $toolName"))
          yield result

        (for
          text   <- loop(prompt, maxToolCalls)
          result <- Async[F].fromEither(decode[A](text).leftMap(e => new RuntimeException(e.getMessage)))
        yield AgentResult.Success(result, name): AgentResult[A])
          .handleError(e => AgentResult.Failure(e.getMessage, name))
