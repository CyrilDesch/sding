package sding.client.api

import com.raquo.laminar.api.L.*
import io.circe.parser.decode
import org.scalajs.dom
import sding.protocol.SseEvent

object SseClient:

  private val allEventTypes =
    List("message", "state", "input_request", "node_complete", "workflow_complete", "error")

  def connect(chatId: String): EventStream[SseEvent] =
    var esRef: Option[dom.EventSource] = None

    EventStream.fromCustomSource[SseEvent](
      shouldStart = _ => true,
      start = (fireValue, fireError, _, _) =>
        val es = new dom.EventSource(s"/api/chat/$chatId/stream")
        esRef = Some(es)
        allEventTypes.foreach { evtType =>
          es.addEventListener(
            evtType,
            (e: dom.MessageEvent) =>
              decode[SseEvent](e.data.asInstanceOf[String]) match
                case Right(event) => fireValue(event)
                case Left(err)    => fireError(new Exception(s"Parse error: ${err.getMessage}"))
          )
        }
        es.onerror = (_: dom.Event) => fireError(new Exception("SSE connection error"))
      ,
      stop = _ => esRef.foreach(_.close())
    )
