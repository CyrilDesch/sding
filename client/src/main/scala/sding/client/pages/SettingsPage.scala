package sding.client.pages

import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.*
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Failure
import scala.util.Success
import sding.client.api.HttpClient
import sding.client.components.Navbar
import sding.protocol.LlmProvider

object SettingsPage:

  def render(): HtmlElement =
    val provider   = Var("Gemini")
    val apiKey     = Var("")
    val model      = Var("")
    val isLoading  = Var(false)
    val isSaving   = Var(false)
    val errorMsg   = Var(Option.empty[String])
    val successMsg = Var(Option.empty[String])
    val keyHint    = Var(Option.empty[String])

    val defaultModels = Map(
      "Gemini"    -> "gemini-2.0-flash",
      "OpenAI"    -> "gpt-4o",
      "Anthropic" -> "claude-sonnet-4-20250514"
    )

    isLoading.set(true)
    HttpClient.getLlmConfig().onComplete {
      case Success(Right(cfg)) =>
        provider.set(cfg.provider.toString)
        model.set(cfg.model)
        keyHint.set(Some(cfg.keyHint))
        isLoading.set(false)
      case Success(Left(_)) =>
        model.set(defaultModels.getOrElse(provider.now(), ""))
        isLoading.set(false)
      case Failure(ex) =>
        errorMsg.set(Some(ex.getMessage))
        isLoading.set(false)
    }

    div(
      cls := "min-h-screen bg-gray-50",
      Navbar.render(),
      div(
        cls := "max-w-2xl mx-auto px-6 py-10",
        h1(cls := "text-2xl font-bold text-gray-900 mb-2", "Settings"),
        p(cls  := "text-gray-500 mb-8", "Configure your LLM provider to start brainstorming."),
        div(
          cls := "bg-white rounded-2xl shadow-sm border border-gray-200 p-8",
          child.maybe <-- isLoading.signal.map(
            if _ then
              Some(
                div(cls := "text-center py-8 text-gray-400", "Loading configuration...")
              )
            else None
          ),
          child.maybe <-- isLoading.signal.map(
            if _ then None
            else
              Some(
                form(
                  L.onSubmit.preventDefault --> { _ =>
                    isSaving.set(true)
                    errorMsg.set(None)
                    successMsg.set(None)
                    val prov = LlmProvider.values.find(_.toString == provider.now()).getOrElse(LlmProvider.Gemini)
                    val key  = apiKey.now()
                    val mdl  = if model.now().isEmpty then defaultModels.getOrElse(provider.now(), "") else model.now()
                    HttpClient.saveLlmConfig(prov, key, mdl).onComplete {
                      case Success(resp) =>
                        isSaving.set(false)
                        keyHint.set(Some(resp.keyHint))
                        apiKey.set("")
                        successMsg.set(Some("Configuration saved successfully!"))
                      case Failure(ex) =>
                        isSaving.set(false)
                        errorMsg.set(Some(ex.getMessage))
                    }
                  },
                  div(
                    cls := "mb-6",
                    label(cls := "block text-sm font-medium text-gray-700 mb-2", "LLM Provider"),
                    div(
                      cls := "grid grid-cols-3 gap-3",
                      List("Gemini", "OpenAI", "Anthropic").map { p =>
                        button(
                          typ := "button",
                          cls <-- provider.signal.map { sel =>
                            val base = "px-4 py-3 rounded-xl text-sm font-medium border-2 transition-all"
                            if sel == p then s"$base border-indigo-500 bg-indigo-50 text-indigo-700"
                            else s"$base border-gray-200 bg-white text-gray-600 hover:border-gray-300"
                          },
                          onClick --> { _ =>
                            provider.set(p)
                            model.set(defaultModels.getOrElse(p, ""))
                          },
                          p
                        )
                      }
                    )
                  ),
                  div(
                    cls := "mb-6",
                    label(cls := "block text-sm font-medium text-gray-700 mb-1.5", "API Key"),
                    child.maybe <-- keyHint.signal.map(
                      _.map(h => p(cls := "text-xs text-gray-400 mb-2", s"Current key: $h"))
                    ),
                    input(
                      cls := "w-full px-4 py-3 border border-gray-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent text-sm font-mono",
                      typ         := "password",
                      placeholder := "Enter your API key",
                      required    := true,
                      controlled(value <-- apiKey.signal, onInput.mapToValue --> apiKey)
                    )
                  ),
                  div(
                    cls := "mb-6",
                    label(cls := "block text-sm font-medium text-gray-700 mb-1.5", "Model"),
                    input(
                      cls := "w-full px-4 py-3 border border-gray-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent text-sm",
                      placeholder := "e.g. gemini-2.0-flash",
                      controlled(value <-- model.signal, onInput.mapToValue --> model)
                    ),
                    p(
                      cls := "text-xs text-gray-400 mt-1.5",
                      "Leave empty for the default model of the selected provider."
                    )
                  ),
                  child.maybe <-- errorMsg.signal.map(
                    _.map(msg =>
                      div(cls := "mb-4 p-3 rounded-lg bg-red-50 text-red-600 text-sm border border-red-200", msg)
                    )
                  ),
                  child.maybe <-- successMsg.signal.map(
                    _.map(msg =>
                      div(cls := "mb-4 p-3 rounded-lg bg-green-50 text-green-600 text-sm border border-green-200", msg)
                    )
                  ),
                  button(
                    cls <-- isSaving.signal.map { saving =>
                      val base = "w-full py-3 rounded-xl text-sm font-semibold transition-all"
                      if saving then s"$base bg-gray-400 text-white cursor-wait"
                      else s"$base bg-indigo-600 text-white hover:bg-indigo-700 shadow-sm hover:shadow"
                    },
                    disabled <-- isSaving.signal,
                    typ := "submit",
                    child.text <-- isSaving.signal.map(if _ then "Saving..." else "Save Configuration")
                  )
                )
              )
          )
        )
      )
    )
