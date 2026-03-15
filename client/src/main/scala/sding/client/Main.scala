package sding.client

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import sding.client.components.Navbar
import sding.client.pages.*

object Main:

  def main(args: Array[String]): Unit =
    Router.init()

    val app = div(
      child <-- Router.currentPage.signal.combineWith(AuthState.isLoggedIn).map { (page, loggedIn) =>
        page match
          case Page.Login     => LoginPage.render()
          case Page.Register  => RegisterPage.render()
          case _ if !loggedIn =>
            LoginPage.render()
          case Page.Landing      => withNavbar(LandingPage.render())
          case Page.Settings     => SettingsPage.render()
          case Page.Chat(chatId) => withNavbar(ChatPage.render(chatId))
      }
    )

    renderOnDomContentLoaded(dom.document.getElementById("app"), app)

  private def withNavbar(content: HtmlElement): HtmlElement =
    div(Navbar.render(), content)
