package sding.agent

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import sding.domain.AppError

class PromptLoaderSpec extends AsyncWordSpec with AsyncIOSpec with Matchers:

  "PromptLoader" should {

    "load a system prompt by name" in {
      for
        loader <- LivePromptLoader.make[IO]
        prompt <- loader.loadSystemPrompt("CreativePMAgent")
      yield prompt should include("Creative Product Manager")
    }

    "load a task prompt by name" in {
      for
        loader <- LivePromptLoader.make[IO]
        pt     <- loader.loadTaskPrompt("EmpathyMapTask")
      yield {
        pt.name shouldBe "EmpathyMapTask"
        pt.template should include("empathy map")
      }
    }

    "fail with AgentError for unknown system prompt name" in {
      for
        loader <- LivePromptLoader.make[IO]
        result <- loader.loadSystemPrompt("NonExistentAgent").attempt
      yield result match
        case Left(_: AppError.AgentError) => succeed
        case other                        => fail(s"Expected AgentError, got $other")
    }

    "fail with AgentError for unknown task prompt name" in {
      for
        loader <- LivePromptLoader.make[IO]
        result <- loader.loadTaskPrompt("NonExistentTask").attempt
      yield result match
        case Left(_: AppError.AgentError) => succeed
        case other                        => fail(s"Expected AgentError, got $other")
    }
  }

  "PromptTemplate" should {

    "render template variables" in {
      val pt = PromptTemplate("test", "Hello {{ name }}, welcome to {{ place }}!", version = 1)
      pt.render(Map("name" -> "Alice", "place" -> "Wonderland")) shouldBe
        "Hello Alice, welcome to Wonderland!"
    }

    "handle variables without spaces around braces" in {
      val pt = PromptTemplate("test", "Hello {{name}}!", version = 1)
      pt.render(Map("name" -> "Bob")) shouldBe "Hello Bob!"
    }

    "leave unmatched variables unchanged" in {
      val pt = PromptTemplate("test", "Hello {{ name }}, {{ unknown }}!", version = 1)
      pt.render(Map("name" -> "Alice")) shouldBe "Hello Alice, {{ unknown }}!"
    }
  }
