package sding.client.pages

import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.*
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Failure
import scala.util.Success
import sding.client.AuthState
import sding.client.Page
import sding.client.Router
import sding.client.api.HttpClient

object RegisterPage:

  def render(): HtmlElement =
    val firstName = Var("")
    val lastName  = Var("")
    val email     = Var("")
    val password  = Var("")
    val isLoading = Var(false)
    val errorMsg  = Var(Option.empty[String])

    div(
      cls := "min-h-screen bg-gradient-to-br from-indigo-50 via-white to-purple-50 flex items-center justify-center px-4",
      div(
        cls := "w-full max-w-md",
        div(
          cls := "text-center mb-8",
          div(
            cls := "w-14 h-14 mx-auto mb-4 rounded-2xl bg-indigo-600 flex items-center justify-center shadow-lg",
            span(cls := "text-white text-xl font-bold", "B")
          ),
          h1(cls := "text-3xl font-bold text-gray-900", "Create account"),
          p(cls  := "text-gray-500 mt-2", "Start brainstorming in minutes")
        ),
        div(
          cls := "bg-white rounded-2xl shadow-sm border border-gray-200 p-8",
          form(
            L.onSubmit.preventDefault --> { _ =>
              isLoading.set(true)
              errorMsg.set(None)
              HttpClient.register(email.now(), password.now(), firstName.now(), lastName.now()).onComplete {
                case Success(resp) =>
                  AuthState.login(resp.token)
                  Router.navigateTo(Page.Settings)
                case Failure(ex) =>
                  isLoading.set(false)
                  errorMsg.set(Some(ex.getMessage))
              }
            },
            div(
              cls := "grid grid-cols-2 gap-4",
              formField("First name", "text", "First name", firstName),
              formField("Last name", "text", "Last name", lastName)
            ),
            formField("Email", "email", "you@example.com", email),
            formField("Password", "password", "Min 8 characters", password),
            child.maybe <-- errorMsg.signal.map(
              _.map(msg => div(cls := "mb-4 p-3 rounded-lg bg-red-50 text-red-600 text-sm border border-red-200", msg))
            ),
            button(
              cls <-- isLoading.signal.map { loading =>
                val base = "w-full py-3 rounded-xl text-sm font-semibold transition-all"
                if loading then s"$base bg-gray-400 text-white cursor-wait"
                else s"$base bg-indigo-600 text-white hover:bg-indigo-700 shadow-sm hover:shadow"
              },
              disabled <-- isLoading.signal,
              typ := "submit",
              child.text <-- isLoading.signal.map(if _ then "Creating account..." else "Create account")
            )
          ),
          div(
            cls := "mt-6 text-center text-sm text-gray-500",
            "Already have an account? ",
            a(
              cls := "text-indigo-600 font-medium hover:text-indigo-700 cursor-pointer",
              onClick --> { _ => Router.navigateTo(Page.Login) },
              "Sign in"
            )
          )
        )
      )
    )

  private def formField(
      labelText: String,
      inputType: String,
      placeholderText: String,
      variable: Var[String]
  ): HtmlElement =
    div(
      cls := "mb-5",
      label(cls := "block text-sm font-medium text-gray-700 mb-1.5", labelText),
      input(
        cls := "w-full px-4 py-3 border border-gray-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent text-sm",
        typ         := inputType,
        placeholder := placeholderText,
        required    := true,
        controlled(value <-- variable.signal, onInput.mapToValue --> variable)
      )
    )
