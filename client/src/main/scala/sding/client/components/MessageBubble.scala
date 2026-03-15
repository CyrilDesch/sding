package sding.client.components

import com.raquo.laminar.api.L.*

object MessageBubble:

  def agent(content: String, nodeName: Option[String]): HtmlElement =
    div(
      cls := "flex justify-start mb-4",
      div(
        cls := "max-w-2xl px-5 py-3 rounded-2xl rounded-tl-sm bg-white shadow-sm border border-gray-100",
        nodeName.map(n =>
          span(
            cls := "text-xs font-medium text-indigo-500 block mb-1",
            n.replace("_", " ").capitalize
          )
        ),
        p(cls := "text-gray-800 text-sm leading-relaxed whitespace-pre-wrap", content)
      )
    )

  def stateUpdate(content: String): HtmlElement =
    div(
      cls := "flex justify-center mb-3",
      div(
        cls := "px-4 py-2 rounded-full bg-gray-100 text-gray-500 text-xs font-medium",
        content
      )
    )

  def error(message: String): HtmlElement =
    div(
      cls := "flex justify-center mb-4",
      div(
        cls := "px-4 py-2 rounded-lg bg-red-50 text-red-600 text-sm border border-red-200",
        s"Error: $message"
      )
    )
