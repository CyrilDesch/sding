package sding.workflow.io

import sding.protocol.SelectionItem

enum UserInputRequest:
  case FreeText(prompt: String)
  case Choice(prompt: String, options: List[String])

trait ChatContext[F[_]]:
  def sendMessage(message: String, format: MessageFormat = MessageFormat.Text): F[Unit]
  def sendState(message: String): F[Unit]
  def requestInput(request: UserInputRequest): F[String]
  def requestSelection(title: String, items: List[SelectionItem], allowRetry: Boolean): F[String]

enum MessageFormat:
  case Text, Html, Markdown
