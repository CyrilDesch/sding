package sding.client.api

import io.circe.parser.decode
import io.circe.syntax.*
import org.scalajs.dom
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.Thenable.Implicits.*
import sding.client.AuthState
import sding.protocol.*

object HttpClient:

  private val baseUrl = "/api"

  private def authHeaders: js.Dictionary[String] =
    val h = js.Dictionary("Content-Type" -> "application/json")
    AuthState.token.now().foreach(t => h("Authorization") = s"Bearer $t")
    h

  private def handleResponse[B: io.circe.Decoder](resp: dom.Response): Future[B] =
    resp.text().toFuture.flatMap { text =>
      if resp.ok then Future.fromTry(decode[B](text).toTry)
      else
        val err = decode[ErrorResponse](text).map(_.error).getOrElse(text)
        Future.failed(new Exception(err))
    }

  private def doPost[B: io.circe.Decoder](url: String, json: String): Future[B] =
    dom
      .fetch(
        s"$baseUrl$url",
        new dom.RequestInit {
          method = dom.HttpMethod.POST
          headers = authHeaders
          body = json
        }
      )
      .flatMap(handleResponse[B])

  private def doGet[B: io.circe.Decoder](url: String): Future[B] =
    dom
      .fetch(
        s"$baseUrl$url",
        new dom.RequestInit {
          method = dom.HttpMethod.GET
          headers = authHeaders
        }
      )
      .flatMap(handleResponse[B])

  private def doPut[B: io.circe.Decoder](url: String, json: String): Future[B] =
    dom
      .fetch(
        s"$baseUrl$url",
        new dom.RequestInit {
          method = dom.HttpMethod.PUT
          headers = authHeaders
          body = json
        }
      )
      .flatMap(handleResponse[B])

  def register(email: String, password: String, firstName: String, lastName: String): Future[AuthTokenResponse] =
    doPost[AuthTokenResponse]("/auth/register", RegisterRequest(email, password, firstName, lastName).asJson.noSpaces)

  def login(email: String, password: String): Future[AuthTokenResponse] =
    doPost[AuthTokenResponse]("/auth/login", LoginRequest(email, password).asJson.noSpaces)

  def createChat(): Future[CreateChatResponse] =
    dom
      .fetch(
        s"$baseUrl/chat",
        new dom.RequestInit {
          method = dom.HttpMethod.POST
          headers = authHeaders
        }
      )
      .flatMap(handleResponse[CreateChatResponse])

  def submitInput(chatId: String, input: String): Future[Unit] =
    val json = SubmitInputRequest(input).asJson.noSpaces
    dom
      .fetch(
        s"$baseUrl/chat/$chatId/input",
        new dom.RequestInit {
          method = dom.HttpMethod.POST
          headers = authHeaders
          body = json
        }
      )
      .map(_ => ())

  def getLlmConfig(): Future[Either[String, LlmConfigResponse]] =
    doGet[LlmConfigResponse]("/user/llm-config")
      .map(Right(_))
      .recover { case e: Exception => Left(e.getMessage) }

  def saveLlmConfig(provider: LlmProvider, apiKey: String, model: String): Future[LlmConfigResponse] =
    doPut[LlmConfigResponse]("/user/llm-config", LlmConfigRequest(provider, apiKey, model).asJson.noSpaces)
