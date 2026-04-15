package chat4s.io

import io.circe.Decoder
import io.circe.Encoder

final case class SelectionDetail(label: String, value: String) derives Decoder, Encoder.AsObject

final case class SelectionItem(
    id: String,
    label: String,
    description: Option[String],
    score: Option[Double],
    tags: List[String],
    details: List[SelectionDetail]
) derives Decoder,
      Encoder.AsObject

enum UserInputRequest:
  case FreeText(prompt: String)
  case Choice(prompt: String, options: List[String])

enum MessageFormat:
  case Text, Html, Markdown

trait ChatContext[F[_]]:
  def sessionId: String
  def sendMessage(message: String, format: MessageFormat = MessageFormat.Text): F[Unit]
  def sendState(message: String): F[Unit]
  def requestInput(request: UserInputRequest): F[String]
  def requestSelection(title: String, items: List[SelectionItem], allowRetry: Boolean): F[String]
