#!/usr/bin/env scala-cli

//> using scala 3.8.2
//> using jvm 21
//> using dep "org.snakeyaml:snakeyaml-engine:3.0.1"

/** Reads prompts.yaml and upserts every prompt into a self-hosted Langfuse
  * instance via the public API (POST /api/public/v2/prompts).
  *
  * Configuration (env vars, all optional — defaults target the local docker-compose stack):
  *   LANGFUSE_BASE_URL   – default: http://localhost:3000
  *   LANGFUSE_PUBLIC_KEY – default: pk-lf-sding-local-public-key
  *   LANGFUSE_SECRET_KEY – default: sk-lf-sding-local-secret-key
  *   PROMPTS_YAML_PATH   – default: server/src/main/resources/prompts.yaml
  *
  * Run via Make:  make langfuse-init-prompts
  * Run directly: scala-cli scripts/langfuse-init-prompts.scala
  */

import java.io.FileInputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers
import java.util as ju
import org.snakeyaml.engine.v2.api.Load
import org.snakeyaml.engine.v2.api.LoadSettings
import scala.jdk.CollectionConverters.*

// ── Config ──────────────────────────────────────────────────────────────────

val baseUrl    = sys.env.getOrElse("LANGFUSE_BASE_URL", "http://localhost:3000")
val publicKey  = sys.env.getOrElse("LANGFUSE_PUBLIC_KEY", "pk-lf-sding-local-public-key")
val secretKey  = sys.env.getOrElse("LANGFUSE_SECRET_KEY", "sk-lf-sding-local-secret-key")
val yamlPath   = sys.env.getOrElse("PROMPTS_YAML_PATH", "server/src/main/resources/prompts.yaml")

val authHeader =
  "Basic " + java.util.Base64.getEncoder.encodeToString(s"$publicKey:$secretKey".getBytes)

// ── Parse prompts.yaml ───────────────────────────────────────────────────────

@SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
def loadYaml(): Map[String, Map[String, String]] =
  val is  = new FileInputStream(yamlPath)
  val raw =
    try
      val yaml = new Load(LoadSettings.builder().build())
      yaml.loadFromInputStream(is).asInstanceOf[ju.Map[String, Object]]
    finally is.close()
  raw.asScala.toMap.map { case (section, entries) =>
    section -> entries.asInstanceOf[ju.Map[String, Object]].asScala.toMap.map { case (k, v) =>
      k -> v.toString
    }
  }

// ── Langfuse API call ─────────────────────────────────────────────────────────

val http = HttpClient.newBuilder()
  .version(java.net.http.HttpClient.Version.HTTP_1_1)
  .build()

def escapeJson(s: String): String =
  s.replace("\\", "\\\\")
   .replace("\"", "\\\"")
   .replace("\n", "\\n")
   .replace("\r", "\\r")
   .replace("\t", "\\t")

def upsertPrompt(name: String, promptText: String, labels: List[String]): (Int, String) =
  val labelsJson = labels.map(l => s"\"$l\"").mkString("[", ",", "]")
  val body       = s"""{"name":"${escapeJson(name)}","type":"text","prompt":"${escapeJson(promptText)}","labels":$labelsJson}"""
  val request    = HttpRequest
    .newBuilder()
    .uri(URI.create(s"$baseUrl/api/public/v2/prompts"))
    .header("Content-Type", "application/json")
    .header("Authorization", authHeader)
    .POST(BodyPublishers.ofString(body))
    .build()
  @annotation.tailrec
  def attempt(retriesLeft: Int): (Int, String) =
    try
      val response = http.send(request, BodyHandlers.ofString())
      (response.statusCode(), response.body())
    catch
      case e: java.io.IOException if retriesLeft > 0 =>
        Thread.sleep(1000)
        attempt(retriesLeft - 1)
  attempt(3)

// ── Main ─────────────────────────────────────────────────────────────────────

@main def main(): Unit =
  println(s"Loading prompts from: $yamlPath")
  println(s"Langfuse target:      $baseUrl")
  println()

  val sections = loadYaml()
  var ok        = 0
  var failed    = 0

  for
    (section, prompts) <- sections
    (name, text)       <- prompts
  do
    val labels = List("production")
    print(s"  [$section] $name ... ")
    val (status, body) = upsertPrompt(name, text, labels)
    if status == 200 || status == 201 then
      println(s"OK ($status)")
      ok += 1
    else
      println(s"FAILED ($status): $body")
      failed += 1

  println()
  println(s"Done: $ok uploaded, $failed failed.")
  if failed > 0 then
    println("Tip: make sure Langfuse is running (`make langfuse`) and the API keys are correct.")
    sys.exit(1)
