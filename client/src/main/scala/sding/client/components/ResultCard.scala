package sding.client.components

import com.raquo.laminar.api.L.*

object ResultCard:

  def workflowComplete(@scala.annotation.unused chatId: String, report: Signal[Option[String]]): HtmlElement =
    div(
      cls := "my-6",
      div(
        cls := "flex justify-center",
        div(
          cls := "max-w-md text-center p-8 bg-gradient-to-br from-green-50 to-emerald-50 rounded-2xl border border-green-200 shadow-sm",
          div(
            cls := "w-12 h-12 mx-auto mb-4 rounded-full bg-green-100 flex items-center justify-center",
            span(cls := "text-green-600 text-xl", "✓")
          ),
          h3(cls := "text-lg font-semibold text-green-800 mb-2", "Brainstorming Complete!"),
          p(cls  := "text-sm text-green-600", "Your project analysis and report have been generated.")
        )
      ),
      child.maybe <-- report.map(_.map(renderReport))
    )

  def nodeComplete(nodeName: String): HtmlElement =
    div(
      cls := "flex justify-center mb-2",
      div(
        cls := "px-3 py-1.5 rounded-full bg-green-50 text-green-600 text-xs font-medium border border-green-100",
        s"✓ ${nodeName.replace("_", " ").capitalize} completed"
      )
    )

  private def renderReport(markdown: String): HtmlElement =
    div(
      cls := "mt-6 max-w-3xl mx-auto",
      div(
        cls := "bg-white rounded-2xl shadow-sm border border-gray-200 overflow-hidden",
        div(
          cls := "bg-gray-50 px-6 py-4 border-b border-gray-200 flex items-center justify-between",
          h3(cls := "text-sm font-semibold text-gray-800", "Generated Report"),
          button(
            cls := "text-xs px-3 py-1.5 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors font-medium",
            onClick --> { _ => copyToClipboard(markdown) },
            "Copy Markdown"
          )
        ),
        div(
          cls := "px-6 py-6 prose prose-sm max-w-none",
          markdown
            .split("\n")
            .map { line =>
              if line.startsWith("# ") then
                h2(cls := "text-xl font-bold text-gray-900 mt-6 mb-3", line.stripPrefix("# "))
              else if line.startsWith("## ") then
                h3(cls := "text-lg font-semibold text-gray-800 mt-5 mb-2", line.stripPrefix("## "))
              else if line.startsWith("### ") then
                h4(cls := "text-base font-medium text-gray-700 mt-4 mb-1", line.stripPrefix("### "))
              else if line.startsWith("- ") then
                div(
                  cls := "flex gap-2 ml-4 my-1",
                  span(cls := "text-gray-400", "•"),
                  span(cls := "text-sm text-gray-700", line.stripPrefix("- "))
                )
              else if line.trim.isEmpty then div(cls := "h-3")
              else p(cls                             := "text-sm text-gray-700 leading-relaxed my-1", line)
            }
            .toSeq
        )
      )
    )

  private def copyToClipboard(text: String): Unit =
    org.scalajs.dom.window.navigator.clipboard.writeText(text)
