package sding.agent

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import io.circe.Decoder
import io.circe.Encoder
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

final case class TestOutput(answer: String) derives Decoder, Encoder.AsObject

class AgentSpec extends AsyncWordSpec with AsyncIOSpec with Matchers:

  private def stubLlmClient(response: String): LlmClient[IO] = new LlmClient[IO]:
    def chat(systemPrompt: String, userPrompt: String, jsonMode: Boolean): IO[String] =
      IO.pure(response)
    def chatWithTools(
        systemPrompt: String,
        userPrompt: String,
        tools: List[LlmToolSpec]
    ): IO[LlmToolResponse] =
      IO.pure(LlmToolResponse.TextResponse(response))

  private def failingLlmClient(error: Throwable): LlmClient[IO] = new LlmClient[IO]:
    def chat(systemPrompt: String, userPrompt: String, jsonMode: Boolean): IO[String] =
      IO.raiseError(error)
    def chatWithTools(
        systemPrompt: String,
        userPrompt: String,
        tools: List[LlmToolSpec]
    ): IO[LlmToolResponse] =
      IO.raiseError(error)

  private val noopQuota: QuotaManager[IO] = new QuotaManager[IO]:
    def acquireSlot: IO[Unit] = IO.unit

  private val validJson = """{"answer": "42"}"""

  "Agent" should {

    "return Success with parsed result on valid LLM JSON response" in {
      val agent = LiveAgent.make[IO](
        config = AgentConfig("test-agent", "TestPrompt", "model", 0.7, None),
        llmClient = stubLlmClient(validJson),
        systemPrompt = "You are a test agent.",
        quotaManager = noopQuota
      )
      agent.call[TestOutput]("What is the answer?").map {
        case AgentResult.Success(output, name) =>
          output.answer shouldBe "42"
          name shouldBe "test-agent"
        case AgentResult.Failure(msg, _) =>
          fail(s"Expected Success, got Failure: $msg")
      }
    }

    "return Failure when LLM invocation fails" in {
      val agent = LiveAgent.make[IO](
        config = AgentConfig("test-agent", "TestPrompt", "model", 0.7, None),
        llmClient = failingLlmClient(new RuntimeException("LLM down")),
        systemPrompt = "You are a test agent.",
        quotaManager = noopQuota
      )
      agent.call[TestOutput]("What is the answer?").map {
        case AgentResult.Failure(msg, name) =>
          msg should include("LLM down")
          name shouldBe "test-agent"
        case AgentResult.Success(_, _) =>
          fail("Expected Failure, got Success")
      }
    }

    "return Failure when LLM returns invalid JSON" in {
      val agent = LiveAgent.make[IO](
        config = AgentConfig("test-agent", "TestPrompt", "model", 0.7, None),
        llmClient = stubLlmClient("not json at all"),
        systemPrompt = "You are a test agent.",
        quotaManager = noopQuota
      )
      agent.call[TestOutput]("What is the answer?").map {
        case AgentResult.Failure(msg, _) =>
          msg should not be empty
        case AgentResult.Success(_, _) =>
          fail("Expected Failure for invalid JSON")
      }
    }

    "acquire quota slot before each call" in {
      var acquired                        = false
      val trackingQuota: QuotaManager[IO] = new QuotaManager[IO]:
        def acquireSlot: IO[Unit] = IO { acquired = true }

      val agent = LiveAgent.make[IO](
        config = AgentConfig("test-agent", "TestPrompt", "model", 0.7, None),
        llmClient = stubLlmClient(validJson),
        systemPrompt = "You are a test agent.",
        quotaManager = trackingQuota
      )
      agent.call[TestOutput]("prompt").map { _ =>
        acquired shouldBe true
      }
    }
  }
