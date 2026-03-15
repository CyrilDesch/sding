package sding.agent

trait PromptLoader[F[_]]:
  def loadSystemPrompt(name: String): F[String]
  def loadTaskPrompt(name: String): F[PromptTemplate]
