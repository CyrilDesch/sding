package sding.domain

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import scala.util.control.NoStackTrace

class ErrorsSpec extends AnyWordSpec with Matchers:

  "AppError hierarchy" should {
    "extend NoStackTrace" in {
      val error: AppError = AppError.ConfigError.LoadFailed("boom")
      error shouldBe a[NoStackTrace]
      error.getStackTrace shouldBe empty
    }

    "expose a human-readable message via getMessage" in {
      val error = AppError.ConfigError.LoadFailed("config failed to load")
      error.getMessage shouldBe "config failed to load"
    }
  }

  "ConfigError" should {
    "carry the load failure detail" in {
      val err = AppError.ConfigError.LoadFailed("missing required env var")
      err.message shouldBe "missing required env var"
    }

    "format a missing env variable message" in {
      val err = AppError.ConfigError.MissingEnvVariable("LLM_API_KEY")
      err.message shouldBe "Missing required environment variable: LLM_API_KEY"
    }
  }

  "AuthError" should {
    "carry the invalid token detail" in {
      val err = AppError.AuthError.InvalidToken("malformed JWT")
      err.message shouldBe "malformed JWT"
    }

    "carry the token expired detail" in {
      val err = AppError.AuthError.TokenExpired("token expired at 2026-01-01")
      err.message shouldBe "token expired at 2026-01-01"
    }
  }

  "ChatError" should {
    "format the chat not found message with the ID" in {
      val id  = ChatId.fromString("550e8400-e29b-41d4-a716-446655440000")
      val err = AppError.ChatError.ChatNotFound(id)
      err.message should include("550e8400-e29b-41d4-a716-446655440000")
    }

    "format the no active workflow message" in {
      val id  = ChatId.fromString("550e8400-e29b-41d4-a716-446655440000")
      val err = AppError.ChatError.NoActiveWorkflow(id)
      err.message should include("No active workflow")
    }
  }

  "WorkflowError" should {
    "format node execution failure with node name and cause" in {
      val err = AppError.WorkflowError.NodeExecutionFailed("empathy_map", "LLM timeout")
      err.message shouldBe "Node 'empathy_map' failed: LLM timeout"
    }
  }

  "AgentError" should {
    "format LLM invocation failure with agent name" in {
      val err = AppError.AgentError.LlmInvocationFailed("ProductStrategist", "rate limited")
      err.message shouldBe "LLM invocation error in ProductStrategist: rate limited"
    }
  }

  "RepositoryError" should {
    "format entity not found with type and id" in {
      val err = AppError.RepositoryError.NotFound("Chat", "abc-123")
      err.message shouldBe "Chat not found: abc-123"
    }
  }
