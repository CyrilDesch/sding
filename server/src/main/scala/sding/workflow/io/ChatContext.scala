package sding.workflow.io

trait ChatContext[F[_]]:
  def sendMessage(message: String, format: MessageFormat = MessageFormat.Text): F[Unit]
  def sendState(message: String): F[Unit]
  def requestInput(prompt: String, options: Option[List[String]] = None): F[String]

enum MessageFormat:
  case Text, Html, Markdown
