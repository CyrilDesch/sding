package sding.client.components

import com.raquo.laminar.api.L.*
import sding.client.AuthState
import sding.client.Page
import sding.client.Router

object Navbar:

  def render(): HtmlElement =
    htmlTag("nav")(
      cls := "bg-white border-b border-gray-200",
      div(
        cls := "max-w-7xl mx-auto px-6 h-14 flex items-center justify-between",
        div(
          cls := "flex items-center gap-6",
          a(
            cls := "flex items-center gap-2 cursor-pointer",
            onClick --> { _ => Router.navigateTo(Page.Landing) },
            div(
              cls := "w-8 h-8 rounded-lg bg-indigo-600 flex items-center justify-center",
              span(cls := "text-white text-sm font-bold", "S")
            ),
            span(cls := "text-base font-semibold text-gray-900", "sding")
          ),
          child.maybe <-- AuthState.isLoggedIn.map {
            case true =>
              Some(
                div(
                  cls := "flex items-center gap-4",
                  navLink("New session", Page.Landing),
                  navLink("Settings", Page.Settings)
                )
              )
            case false => None
          }
        ),
        child <-- AuthState.isLoggedIn.map {
          case true =>
            button(
              cls := "text-sm text-gray-500 hover:text-gray-700 transition-colors",
              onClick --> { _ => AuthState.logout() },
              "Sign out"
            )
          case false =>
            div(
              cls := "flex items-center gap-3",
              a(
                cls := "text-sm text-gray-500 hover:text-gray-700 cursor-pointer transition-colors",
                onClick --> { _ => Router.navigateTo(Page.Login) },
                "Sign in"
              ),
              a(
                cls := "text-sm px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 cursor-pointer transition-colors font-medium",
                onClick --> { _ => Router.navigateTo(Page.Register) },
                "Sign up"
              )
            )
        }
      )
    )

  private def navLink(text: String, target: Page): HtmlElement =
    a(
      cls := "text-sm text-gray-500 hover:text-gray-700 cursor-pointer transition-colors",
      onClick --> { _ => Router.navigateTo(target) },
      text
    )
