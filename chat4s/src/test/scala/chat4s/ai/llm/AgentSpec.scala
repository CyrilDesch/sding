package chat4s.ai.llm

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.testkit.TestControl
import chat4s.ai.AgentResult
import chat4s.ai.JsonSchemaOf
import chat4s.ai.prompt.PromptLink
import dev.langchain4j.exception.RateLimitException
import dev.langchain4j.model.chat.request.json.JsonObjectSchema
import io.circe.Decoder
import io.circe.Encoder
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.typelevel.otel4s.trace.Tracer

final case class TestOutput(answer: String) derives Decoder, Encoder.AsObject, JsonSchemaOf

class AgentSpec extends AsyncWordSpec with AsyncIOSpec with Matchers:

  given Tracer[IO] = Tracer.noop

  private val testPromptLink = PromptLink("TestPrompt", 1, "test-session")

  private def stubLlmClient(response: String): LlmClient[IO] = new LlmClient[IO]:
    def chatStructured(
        systemPrompt: String,
        userPrompt: String,
        outputSchema: JsonObjectSchema,
        promptLink: PromptLink
    ): IO[String] =
      IO.pure(response)
    def chatStep(
        systemPrompt: String,
        history: Vector[LlmMessage],
        tools: List[LlmToolSpec],
        promptLink: PromptLink
    ): IO[(LlmToolResponse, Vector[LlmMessage])] =
      IO.pure((LlmToolResponse.TextResponse(response), history :+ LlmMessage.AssistantText(response)))
    def extractStructured(
        systemPrompt: String,
        history: Vector[LlmMessage],
        outputSchema: JsonObjectSchema,
        promptLink: PromptLink
    ): IO[String] =
      IO.pure(response)

  private def failingLlmClient(error: Throwable): LlmClient[IO] = new LlmClient[IO]:
    def chatStructured(
        systemPrompt: String,
        userPrompt: String,
        outputSchema: JsonObjectSchema,
        promptLink: PromptLink
    ): IO[String] =
      IO.raiseError(error)
    def chatStep(
        systemPrompt: String,
        history: Vector[LlmMessage],
        tools: List[LlmToolSpec],
        promptLink: PromptLink
    ): IO[(LlmToolResponse, Vector[LlmMessage])] =
      IO.raiseError(error)
    def extractStructured(
        systemPrompt: String,
        history: Vector[LlmMessage],
        outputSchema: JsonObjectSchema,
        promptLink: PromptLink
    ): IO[String] =
      IO.raiseError(error)

  private val validJson = """{"answer": "42"}"""

  "Agent" should {

    "return Success with parsed result on valid LLM JSON response" in {
      val agent = LiveAgent.make[IO](
        agentConfig = AgentConfig("test-agent", "TestPrompt", "model", 0.7, None),
        llmClient = stubLlmClient(validJson),
        systemPrompt = "You are a test agent."
      )
      agent.call[TestOutput]("What is the answer?", testPromptLink).map {
        case AgentResult.Success(output, name) =>
          output.answer shouldBe "42"
          name shouldBe "test-agent"
        case AgentResult.Failure(msg, _) =>
          fail(s"Expected Success, got Failure: $msg")
      }
    }

    "return Failure when LLM invocation fails" in {
      val agent = LiveAgent.make[IO](
        agentConfig = AgentConfig("test-agent", "TestPrompt", "model", 0.7, None),
        llmClient = failingLlmClient(new RuntimeException("LLM down")),
        systemPrompt = "You are a test agent."
      )
      agent.call[TestOutput]("What is the answer?", testPromptLink).map {
        case AgentResult.Failure(msg, name) =>
          msg should include("LLM down")
          name shouldBe "test-agent"
        case AgentResult.Success(_, _) =>
          fail("Expected Failure, got Success")
      }
    }

    "return Failure when LLM returns invalid JSON" in {
      val agent = LiveAgent.make[IO](
        agentConfig = AgentConfig("test-agent", "TestPrompt", "model", 0.7, None),
        llmClient = stubLlmClient("not json at all"),
        systemPrompt = "You are a test agent."
      )
      agent.call[TestOutput]("What is the answer?", testPromptLink).map {
        case AgentResult.Failure(msg, _) =>
          msg should not be empty
        case AgentResult.Success(_, _) =>
          fail("Expected Failure for invalid JSON")
      }
    }

    "retry call on RateLimitException and succeed on next attempt" in {
      IO.ref(0).flatMap { callCount =>
        val client = new LlmClient[IO]:
          def chatStructured(s: String, u: String, o: JsonObjectSchema, pl: PromptLink): IO[String] =
            callCount.updateAndGet(_ + 1).flatMap {
              case 1 => IO.raiseError(new RateLimitException("rate limit"))
              case _ => IO.pure(validJson)
            }
          def chatStep(
              s: String,
              h: Vector[LlmMessage],
              t: List[LlmToolSpec],
              pl: PromptLink
          ): IO[(LlmToolResponse, Vector[LlmMessage])] =
            IO.pure((LlmToolResponse.TextResponse(""), h))
          def extractStructured(s: String, h: Vector[LlmMessage], o: JsonObjectSchema, pl: PromptLink): IO[String] =
            IO.pure(validJson)

        val agent = LiveAgent.make[IO](
          agentConfig = AgentConfig("test-agent", "TestPrompt", "model", 0.7, None),
          llmClient = client,
          systemPrompt = "You are a test agent."
        )
        TestControl.executeEmbed(
          agent.call[TestOutput]("What is the answer?", testPromptLink).map {
            case AgentResult.Success(output, _) =>
              output.answer shouldBe "42"
            case AgentResult.Failure(msg, _) =>
              fail(s"Expected Success after retry, got Failure: $msg")
          }
        )
      }
    }

    "return Failure with rate limit message after exhausting retries" in {
      val agent = LiveAgent.make[IO](
        agentConfig = AgentConfig("test-agent", "TestPrompt", "model", 0.7, None),
        llmClient = failingLlmClient(new RateLimitException("rate limit")),
        systemPrompt = "You are a test agent."
      )
      TestControl.executeEmbed(
        agent.call[TestOutput]("What is the answer?", testPromptLink).map {
          case AgentResult.Failure(msg, _) =>
            msg should include("rate limit exceeded")
          case AgentResult.Success(_, _) =>
            fail("Expected Failure after exhausting retries")
        }
      )
    }

    "tooledCall retries chatStep on RateLimitException preserving history" in {
      IO.ref(0).flatMap { chatStepCount =>
        val client = new LlmClient[IO]:
          def chatStructured(s: String, u: String, o: JsonObjectSchema, pl: PromptLink): IO[String] =
            IO.pure(validJson)
          def chatStep(
              s: String,
              h: Vector[LlmMessage],
              t: List[LlmToolSpec],
              pl: PromptLink
          ): IO[(LlmToolResponse, Vector[LlmMessage])] =
            chatStepCount.updateAndGet(_ + 1).flatMap {
              case 1 => IO.raiseError(new RateLimitException("rate limit"))
              case _ => IO.pure((LlmToolResponse.TextResponse("done"), h :+ LlmMessage.AssistantText("done")))
            }
          def extractStructured(s: String, h: Vector[LlmMessage], o: JsonObjectSchema, pl: PromptLink): IO[String] =
            IO.pure(validJson)

        val agent = LiveAgent.make[IO](
          agentConfig = AgentConfig("test-agent", "TestPrompt", "model", 0.7, None),
          llmClient = client,
          systemPrompt = "You are a test agent."
        )
        TestControl.executeEmbed(
          agent.tooledCall[TestOutput]("What is the answer?", Nil, testPromptLink).map {
            case AgentResult.Success(output, _) =>
              output.answer shouldBe "42"
            case AgentResult.Failure(msg, _) =>
              fail(s"Expected Success after chatStep retry, got Failure: $msg")
          }
        )
      }
    }
  }
