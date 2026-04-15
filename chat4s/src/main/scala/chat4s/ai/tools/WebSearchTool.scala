package chat4s.ai.tools

import cats.effect.Async
import chat4s.ai.AgentTool
import chat4s.ai.SchemaElement

trait WebSearchTool[F[_]] extends AgentTool[F]:
  def name: String        = "web_search"
  def description: String = "Search the web for information about a given topic. Returns relevant snippets."
  override def inputSchema: Option[SchemaElement.JsObject] =
    Some(SchemaElement.JsObject(Map("query" -> SchemaElement.JsString), List("query")))

object WebSearchTool:

  def stub[F[_]: Async]: WebSearchTool[F] = new WebSearchTool[F]:
    def execute(input: String): F[String] =
      Async[F].pure(
        s"""[{"title":"Market analysis for: $input","snippet":"Growing demand observed in this space with multiple indicators of user interest.","url":"https://example.com/analysis"},{"title":"User complaints about: $input","snippet":"Several forums show users experiencing pain points related to this problem domain.","url":"https://example.com/forums"}]"""
      )
