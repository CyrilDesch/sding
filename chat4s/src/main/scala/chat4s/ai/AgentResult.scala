package chat4s.ai

import io.circe.Encoder

enum AgentResult[+A]:
  case Success(value: A, agentName: String)
  case Failure(errorMessage: String, agentName: String)

object AgentResult:
  given [A: Encoder]: Encoder[AgentResult[A]] = Encoder.instance:
    case AgentResult.Success(value, name) =>
      io.circe.Json.obj(
        "status"     -> io.circe.Json.fromString("success"),
        "value"      -> Encoder[A].apply(value),
        "agent_name" -> io.circe.Json.fromString(name)
      )
    case AgentResult.Failure(msg, name) =>
      io.circe.Json.obj(
        "status"        -> io.circe.Json.fromString("failure"),
        "error_message" -> io.circe.Json.fromString(msg),
        "agent_name"    -> io.circe.Json.fromString(name)
      )
