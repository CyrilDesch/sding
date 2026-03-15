package sding.client.pages

import com.raquo.laminar.api.L.*
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Failure
import scala.util.Success
import sding.client.Page
import sding.client.Router
import sding.client.api.HttpClient

object LandingPage:

  def render(): HtmlElement =
    val isLoading = Var(false)
    val errorMsg  = Var(Option.empty[String])

    div(
      cls := "flex-1 flex items-center justify-center min-h-[calc(100vh-3.5rem)]",
      div(
        cls := "max-w-2xl mx-auto text-center px-6",
        div(
          cls := "mb-8",
          div(
            cls := "w-16 h-16 mx-auto mb-6 rounded-2xl bg-indigo-600 flex items-center justify-center shadow-lg",
            span(cls := "text-white text-2xl font-bold", "B")
          ),
          h1(
            cls := "text-5xl font-bold text-gray-900 mb-4 tracking-tight",
            "Brainstormer"
          ),
          p(
            cls := "text-xl text-gray-500 max-w-lg mx-auto leading-relaxed",
            "AI-powered product brainstorming. From problem discovery to validated prototypes."
          )
        ),
        div(
          cls := "mb-12",
          div(
            cls := "grid grid-cols-3 gap-6 max-w-lg mx-auto",
            featureChip("Problem Discovery", "Weird problems & trend analysis"),
            featureChip("User Research", "Empathy maps & JTBD"),
            featureChip("Solution Design", "SCAMPER & competitive analysis")
          )
        ),
        button(
          cls <-- isLoading.signal.map { loading =>
            val base =
              "px-8 py-4 rounded-2xl text-lg font-semibold shadow-lg transition-all transform hover:scale-105"
            if loading then s"$base bg-gray-400 text-white cursor-wait"
            else s"$base bg-indigo-600 text-white hover:bg-indigo-700 hover:shadow-xl"
          },
          disabled <-- isLoading.signal,
          onClick --> { _ =>
            isLoading.set(true)
            errorMsg.set(None)
            HttpClient.createChat().onComplete {
              case Success(resp) =>
                isLoading.set(false)
                Router.navigateTo(Page.Chat(resp.chatId))
              case Failure(ex) =>
                isLoading.set(false)
                errorMsg.set(Some(ex.getMessage))
            }
          },
          child.text <-- isLoading.signal.map {
            case true  => "Starting..."
            case false => "Start Brainstorming"
          }
        ),
        child.maybe <-- errorMsg.signal.map(_.map { msg =>
          p(cls := "mt-4 text-red-500 text-sm", msg)
        })
      )
    )

  private def featureChip(title: String, desc: String): HtmlElement =
    div(
      cls := "text-center",
      p(cls := "text-sm font-semibold text-gray-800", title),
      p(cls := "text-xs text-gray-400 mt-1", desc)
    )
