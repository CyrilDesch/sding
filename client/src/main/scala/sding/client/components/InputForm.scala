package sding.client.components

import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.*

object InputForm:

  def textInput(prompt: String, submitFn: String => Unit): HtmlElement =
    val inputVar = Var("")
    div(
      cls := "border-t border-gray-200 bg-white p-4",
      p(cls := "text-sm text-gray-600 mb-3 font-medium", prompt),
      form(
        cls := "flex gap-3",
        L.onSubmit.preventDefault --> { _ =>
          val v = inputVar.now()
          if v.nonEmpty then
            submitFn(v)
            inputVar.set("")
        },
        input(
          cls := "flex-1 px-4 py-3 border border-gray-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent text-sm",
          placeholder := "Type your answer...",
          controlled(value <-- inputVar.signal, onInput.mapToValue --> inputVar),
          autoFocus := true
        ),
        button(
          cls := "px-6 py-3 bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 transition-colors text-sm font-medium shadow-sm",
          typ := "submit",
          "Send"
        )
      )
    )

  def choiceInput(prompt: String, options: List[String], onSelect: String => Unit): HtmlElement =
    div(
      cls := "border-t border-gray-200 bg-white p-4",
      p(cls := "text-sm text-gray-600 mb-3 font-medium", prompt),
      div(
        cls := "flex flex-wrap gap-2",
        options.map { opt =>
          button(
            cls := "px-5 py-2.5 bg-white border-2 border-gray-200 rounded-xl hover:border-indigo-500 hover:bg-indigo-50 transition-all text-sm font-medium text-gray-700",
            onClick --> { _ => onSelect(opt) },
            opt
          )
        }
      )
    )
