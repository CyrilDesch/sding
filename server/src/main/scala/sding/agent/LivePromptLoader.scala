package sding.agent

import cats.effect.Ref
import cats.effect.Sync
import cats.syntax.all.*
import java.io.InputStream
import java.util as ju
import scala.jdk.CollectionConverters.*
import sding.domain.AppError

object LivePromptLoader:
  def make[F[_]: Sync]: F[PromptLoader[F]] =
    loadYaml[F].flatMap { data =>
      Ref.of[F, Map[String, Map[String, String]]](data).map { cacheRef =>
        new PromptLoader[F]:
          def loadSystemPrompt(name: String): F[String] =
            cacheRef.get.flatMap { cache =>
              cache.getOrElse("system_prompts", Map.empty).get(name) match
                case Some(prompt) => Sync[F].pure(prompt)
                case None         =>
                  Sync[F].raiseError(
                    AppError.AgentError.PromptLoadFailed(name, s"System prompt '$name' not found in prompts.yaml")
                  )
            }

          def loadTaskPrompt(name: String): F[PromptTemplate] =
            cacheRef.get.flatMap { cache =>
              val taskPrompts   = cache.getOrElse("task_prompts", Map.empty)
              val systemPrompts = cache.getOrElse("system_prompts", Map.empty)
              taskPrompts.get(name).orElse(systemPrompts.get(name)) match
                case Some(template) => Sync[F].pure(PromptTemplate(name, template, version = 1))
                case None           =>
                  Sync[F].raiseError(
                    AppError.AgentError.PromptLoadFailed(name, s"Prompt '$name' not found in prompts.yaml")
                  )
            }
      }
    }

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  private def loadYaml[F[_]: Sync]: F[Map[String, Map[String, String]]] =
    Sync[F].blocking {
      val is: InputStream = getClass.getClassLoader.getResourceAsStream("prompts.yaml")
      if is == null then
        throw AppError.AgentError.PromptLoadFailed("PromptLoader", "prompts.yaml not found on classpath")
      try
        val settings = org.snakeyaml.engine.v2.api.LoadSettings.builder().build()
        val yaml     = new org.snakeyaml.engine.v2.api.Load(settings)
        val raw      = yaml.loadFromInputStream(is).asInstanceOf[ju.Map[String, Object]]
        raw.asScala.toMap.map { case (section, entries) =>
          val entriesMap = entries.asInstanceOf[ju.Map[String, Object]]
          section -> entriesMap.asScala.toMap.map { case (k, v) => k -> v.toString }
        }
      finally is.close()
    }
