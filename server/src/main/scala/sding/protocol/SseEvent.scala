package sding.protocol

import io.circe.Decoder
import io.circe.Encoder
import io.circe.HCursor
import io.circe.Json
import io.circe.syntax.*

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

enum SseEvent:
  case WorkflowPlan(steps: List[WorkflowStep])
  case Message(content: String, format: String, sourceNode: Option[WorkflowStep])
  case UserMessage(content: String)
  case StateUpdate(content: String, sourceNode: Option[WorkflowStep])
  case InputRequest(prompt: String, options: Option[List[String]], sourceNode: Option[WorkflowStep])
  case SelectionRequest(
      title: String,
      items: List[SelectionItem],
      allowRetry: Boolean,
      sourceNode: Option[WorkflowStep]
  )
  case NodeComplete(step: WorkflowStep)
  case WorkflowComplete(chatId: String)
  case Error(message: String)

object SseEvent:
  def eventType(e: SseEvent): String = e match
    case _: WorkflowPlan     => "workflow_plan"
    case _: Message          => "message"
    case _: UserMessage      => "user_message"
    case _: StateUpdate      => "state"
    case _: InputRequest     => "input_request"
    case _: SelectionRequest => "selection_request"
    case _: NodeComplete     => "node_complete"
    case _: WorkflowComplete => "workflow_complete"
    case _: Error            => "error"

  given Encoder[SseEvent] = Encoder.instance:
    case WorkflowPlan(steps) =>
      Json.obj("type" -> "workflow_plan".asJson, "steps" -> steps.asJson)
    case Message(c, f, sn) =>
      Json.obj("type" -> "message".asJson, "content" -> c.asJson, "format" -> f.asJson, "source_node" -> sn.asJson)
    case UserMessage(c) =>
      Json.obj("type" -> "user_message".asJson, "content" -> c.asJson)
    case StateUpdate(c, sn) =>
      Json.obj("type" -> "state".asJson, "content" -> c.asJson, "source_node" -> sn.asJson)
    case InputRequest(p, opts, sn) =>
      Json.obj(
        "type"        -> "input_request".asJson,
        "prompt"      -> p.asJson,
        "options"     -> opts.asJson,
        "source_node" -> sn.asJson
      )
    case SelectionRequest(title, items, allowRetry, sn) =>
      Json.obj(
        "type"        -> "selection_request".asJson,
        "title"       -> title.asJson,
        "items"       -> items.asJson,
        "allow_retry" -> allowRetry.asJson,
        "source_node" -> sn.asJson
      )
    case NodeComplete(s) =>
      Json.obj("type" -> "node_complete".asJson, "node" -> s.asJson)
    case WorkflowComplete(c) =>
      Json.obj("type" -> "workflow_complete".asJson, "chat_id" -> c.asJson)
    case Error(m) =>
      Json.obj("type" -> "error".asJson, "message" -> m.asJson)

  given Decoder[SseEvent] = Decoder.instance { (c: HCursor) =>
    c.get[String]("type").flatMap {
      case "workflow_plan" =>
        c.get[List[WorkflowStep]]("steps").map(WorkflowPlan.apply)
      case "message" =>
        for
          content    <- c.get[String]("content")
          format     <- c.get[String]("format")
          sourceNode <- c.get[Option[WorkflowStep]]("source_node")
        yield Message(content, format, sourceNode)
      case "user_message" =>
        c.get[String]("content").map(UserMessage.apply)
      case "state" =>
        for
          content    <- c.get[String]("content")
          sourceNode <- c.get[Option[WorkflowStep]]("source_node")
        yield StateUpdate(content, sourceNode)
      case "input_request" =>
        for
          prompt     <- c.get[String]("prompt")
          options    <- c.get[Option[List[String]]]("options")
          sourceNode <- c.get[Option[WorkflowStep]]("source_node")
        yield InputRequest(prompt, options, sourceNode)
      case "selection_request" =>
        for
          title      <- c.get[String]("title")
          items      <- c.get[List[SelectionItem]]("items")
          allowRetry <- c.get[Boolean]("allow_retry")
          sourceNode <- c.get[Option[WorkflowStep]]("source_node")
        yield SelectionRequest(title, items, allowRetry, sourceNode)
      case "node_complete" =>
        c.get[WorkflowStep]("node").map(NodeComplete.apply)
      case "workflow_complete" =>
        c.get[String]("chat_id").map(WorkflowComplete.apply)
      case "error" =>
        c.get[String]("message").map(Error.apply)
      case other =>
        Left(io.circe.DecodingFailure(s"Unknown SseEvent type: $other", c.history))
    }
  }
