package sding.client.pages

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import sding.client.api.HttpClient
import sding.client.api.SseClient
import sding.client.components.*
import sding.protocol.SseEvent

object ChatPage:

  sealed trait ChatItem
  object ChatItem:
    case class AgentMessage(content: String, node: Option[String])  extends ChatItem
    case class State(content: String)                               extends ChatItem
    case class NodeDone(name: String)                               extends ChatItem
    case class Input(prompt: String, options: Option[List[String]]) extends ChatItem
    case class Done(chatId: String)                                 extends ChatItem
    case class Err(message: String)                                 extends ChatItem

  def render(chatId: String): HtmlElement =
    val items         = Var(List.empty[ChatItem])
    val completed     = Var(Set.empty[String])
    val activeNode    = Var(Option.empty[String])
    val pendingInput  = Var(Option.empty[(String, Option[List[String]])])
    val reportContent = Var(Option.empty[String])

    val sseStream = SseClient.connect(chatId)

    div(
      cls       := "flex bg-gray-50",
      styleAttr := "height: calc(100vh - 3.5rem)",

      ProgressBar.render(completed.signal, activeNode.signal),

      div(
        cls := "flex-1 flex flex-col min-w-0",

        div(
          cls := "border-b border-gray-200 bg-white px-6 py-4 flex items-center justify-between flex-shrink-0",
          h2(cls   := "text-lg font-semibold text-gray-800", "Brainstorming Session"),
          span(cls := "text-xs text-gray-400 font-mono", chatId.take(8) + "...")
        ),

        div(
          cls := "flex-1 overflow-y-auto px-6 py-4",
          children <-- items.signal.combineWith(reportContent.signal).map { (list, _) =>
            list.map {
              case ChatItem.AgentMessage(c, n) => MessageBubble.agent(c, n)
              case ChatItem.State(c)           => MessageBubble.stateUpdate(c)
              case ChatItem.NodeDone(n)        => ResultCard.nodeComplete(n)
              case ChatItem.Done(cid)          => ResultCard.workflowComplete(cid, reportContent.signal)
              case ChatItem.Err(m)             => MessageBubble.error(m)
              case ChatItem.Input(_, _)        => emptyNode
            }
          },
          onMountCallback { ctx =>
            sseStream.foreach { event =>
              event match
                case SseEvent.Message(content, _, sourceNode) =>
                  if sourceNode.contains("markdown_generation") then
                    reportContent.update {
                      case Some(existing) => Some(existing + "\n" + content)
                      case None           => Some(content)
                    }
                  items.update(_ :+ ChatItem.AgentMessage(content, sourceNode))
                  scrollToBottom()
                case SseEvent.StateUpdate(content, sourceNode) =>
                  items.update(_ :+ ChatItem.State(content))
                  sourceNode.foreach(n => activeNode.set(Some(n)))
                  scrollToBottom()
                case SseEvent.InputRequest(prompt, options, _) =>
                  pendingInput.set(Some((prompt, options)))
                  scrollToBottom()
                case SseEvent.NodeComplete(name) =>
                  completed.update(_ + name)
                  activeNode.set(None)
                  items.update(_ :+ ChatItem.NodeDone(name))
                  scrollToBottom()
                case SseEvent.WorkflowComplete(cid) =>
                  activeNode.set(None)
                  items.update(_ :+ ChatItem.Done(cid))
                  pendingInput.set(None)
                  scrollToBottom()
                case SseEvent.Error(msg) =>
                  items.update(_ :+ ChatItem.Err(msg))
                  scrollToBottom()
            }(using ctx.owner)
          }
        ),

        child.maybe <-- pendingInput.signal.map {
          case Some((prompt, Some(options))) if options.nonEmpty =>
            Some(InputForm.choiceInput(prompt, options, answer => submitAnswer(chatId, answer, pendingInput, items)))
          case Some((prompt, _)) =>
            Some(InputForm.textInput(prompt, answer => submitAnswer(chatId, answer, pendingInput, items)))
          case None =>
            None
        }
      )
    )

  private def submitAnswer(
      chatId: String,
      answer: String,
      pendingInput: Var[Option[(String, Option[List[String]])]],
      items: Var[List[ChatItem]]
  ): Unit =
    pendingInput.set(None)
    items.update(_ :+ ChatItem.AgentMessage(answer, Some("you")))
    HttpClient.submitInput(chatId, answer)

  private def scrollToBottom(): Unit =
    dom.window.setTimeout(
      () => {
        val container = dom.document.querySelector(".overflow-y-auto")
        if container != null then container.scrollTop = container.scrollHeight
      },
      50
    )
