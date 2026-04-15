package chat4s.ai.prompt

trait PromptLoader[F[_]]:
  def loadSystemPrompt(name: String): F[String]
  def loadTaskPrompt(name: String): F[PromptTemplate]
