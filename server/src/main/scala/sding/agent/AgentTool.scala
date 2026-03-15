package sding.agent

trait AgentTool[F[_]]:
  def name: String
  def description: String
  def execute(input: String): F[String]
