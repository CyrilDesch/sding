package chat4s.ai

trait AgentTool[F[_]]:
  def name: String
  def description: String
  def inputSchema: Option[SchemaElement.JsObject] = None
  def execute(input: String): F[String]
