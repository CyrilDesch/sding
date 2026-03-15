package sding.protocol

import io.circe.Decoder
import io.circe.Encoder
import io.circe.HCursor
import io.circe.Json
import io.circe.syntax.*

enum SseEvent:
  case Message(content: String, format: String, sourceNode: Option[String])
  case StateUpdate(content: String, sourceNode: Option[String])
  case InputRequest(prompt: String, options: Option[List[String]], sourceNode: Option[String])
  case NodeComplete(nodeName: String)
  case WorkflowComplete(chatId: String)
  case Error(message: String)

object SseEvent:
  def eventType(e: SseEvent): String = e match
    case _: Message          => "message"
    case _: StateUpdate      => "state"
    case _: InputRequest     => "input_request"
    case _: NodeComplete     => "node_complete"
    case _: WorkflowComplete => "workflow_complete"
    case _: Error            => "error"

  given Encoder[SseEvent] = Encoder.instance:
    case Message(c, f, sn) =>
      Json.obj("type" -> "message".asJson, "content" -> c.asJson, "format" -> f.asJson, "source_node" -> sn.asJson)
    case StateUpdate(c, sn) =>
      Json.obj("type" -> "state".asJson, "content" -> c.asJson, "source_node" -> sn.asJson)
    case InputRequest(p, opts, sn) =>
      Json.obj(
        "type"        -> "input_request".asJson,
        "prompt"      -> p.asJson,
        "options"     -> opts.asJson,
        "source_node" -> sn.asJson
      )
    case NodeComplete(n) =>
      Json.obj("type" -> "node_complete".asJson, "node" -> n.asJson)
    case WorkflowComplete(c) =>
      Json.obj("type" -> "workflow_complete".asJson, "chat_id" -> c.asJson)
    case Error(m) =>
      Json.obj("type" -> "error".asJson, "message" -> m.asJson)

  given Decoder[SseEvent] = Decoder.instance { (c: HCursor) =>
    c.get[String]("type").flatMap {
      case "message" =>
        for
          content    <- c.get[String]("content")
          format     <- c.get[String]("format")
          sourceNode <- c.get[Option[String]]("source_node")
        yield Message(content, format, sourceNode)
      case "state" =>
        for
          content    <- c.get[String]("content")
          sourceNode <- c.get[Option[String]]("source_node")
        yield StateUpdate(content, sourceNode)
      case "input_request" =>
        for
          prompt     <- c.get[String]("prompt")
          options    <- c.get[Option[List[String]]]("options")
          sourceNode <- c.get[Option[String]]("source_node")
        yield InputRequest(prompt, options, sourceNode)
      case "node_complete" =>
        c.get[String]("node").map(NodeComplete.apply)
      case "workflow_complete" =>
        c.get[String]("chat_id").map(WorkflowComplete.apply)
      case "error" =>
        c.get[String]("message").map(Error.apply)
      case other =>
        Left(io.circe.DecodingFailure(s"Unknown SseEvent type: $other", c.history))
    }
  }
