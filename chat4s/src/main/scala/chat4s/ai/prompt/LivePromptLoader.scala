package chat4s.ai.prompt

import cats.effect.Ref
import cats.effect.Sync
import cats.syntax.all.*
import java.io.InputStream
import java.util as ju
import scala.jdk.CollectionConverters.*

object LivePromptLoader:
  def make[F[_]: Sync](is: InputStream): F[PromptLoader[F]] =
    loadYaml[F](is).flatMap { data =>
      Ref.of[F, Map[String, Map[String, String]]](data).map { cacheRef =>
        new PromptLoader[F]:
          def loadSystemPrompt(name: String): F[String] =
            cacheRef.get.flatMap { cache =>
              cache.getOrElse("system_prompts", Map.empty).get(name) match
                case Some(prompt) => Sync[F].pure(prompt)
                case None         =>
                  Sync[F].raiseError(
                    new PromptLoadError(name, s"System prompt '$name' not found in prompts.yaml")
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
                    new PromptLoadError(name, s"Prompt '$name' not found in prompts.yaml")
                  )
            }
      }
    }

  def makeFromClasspath[F[_]: Sync]: F[PromptLoader[F]] =
    Sync[F].blocking {
      val is = getClass.getClassLoader.getResourceAsStream("prompts.yaml")
      if is == null then
        throw new PromptLoadError("PromptLoader", "prompts.yaml not found on classpath")
      is
    }.flatMap(make[F])

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  private def loadYaml[F[_]: Sync](is: InputStream): F[Map[String, Map[String, String]]] =
    Sync[F].blocking {
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
