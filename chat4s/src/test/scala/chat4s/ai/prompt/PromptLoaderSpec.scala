package chat4s.ai.prompt

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import java.io.ByteArrayInputStream
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

class PromptLoaderSpec extends AsyncWordSpec with AsyncIOSpec with Matchers:

  private val testYaml =
    """|system_prompts:
       |  CreativePMAgent: |
       |    You are a Creative Product Manager. Generate innovative product ideas.
       |task_prompts:
       |  EmpathyMapTask: |
       |    Build a detailed empathy map for the target user of this project.
       |""".stripMargin

  private def makeLoader: IO[PromptLoader[IO]] =
    LivePromptLoader.make[IO](new ByteArrayInputStream(testYaml.getBytes("UTF-8")))

  "PromptLoader" should {

    "load a system prompt by name" in {
      for
        loader <- makeLoader
        prompt <- loader.loadSystemPrompt("CreativePMAgent")
      yield prompt should include("Creative Product Manager")
    }

    "load a task prompt by name" in {
      for
        loader <- makeLoader
        pt     <- loader.loadTaskPrompt("EmpathyMapTask")
      yield {
        pt.name shouldBe "EmpathyMapTask"
        pt.template should include("empathy map")
      }
    }

    "fail with PromptLoadError for unknown system prompt name" in {
      for
        loader <- makeLoader
        result <- loader.loadSystemPrompt("NonExistentAgent").attempt
      yield result match
        case Left(_: PromptLoadError) => succeed
        case other                    => fail(s"Expected PromptLoadError, got $other")
    }

    "fail with PromptLoadError for unknown task prompt name" in {
      for
        loader <- makeLoader
        result <- loader.loadTaskPrompt("NonExistentTask").attempt
      yield result match
        case Left(_: PromptLoadError) => succeed
        case other                    => fail(s"Expected PromptLoadError, got $other")
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
