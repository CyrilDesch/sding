package sding.client

import com.raquo.laminar.api.L.*
import org.scalajs.dom

enum Page:
  case Landing
  case Login
  case Register
  case Settings
  case Chat(chatId: String)

object Router:
  val currentPage: Var[Page] = Var(parsePage(dom.window.location.hash))

  def navigateTo(page: Page): Unit =
    val hash = toHash(page)
    dom.window.location.hash = hash
    currentPage.set(page)

  def init(): Unit =
    windowEvents(_.onHashChange).foreach { _ =>
      currentPage.set(parsePage(dom.window.location.hash))
    }(using unsafeWindowOwner)

  private def toHash(page: Page): String = page match
    case Page.Landing      => ""
    case Page.Login        => "#login"
    case Page.Register     => "#register"
    case Page.Settings     => "#settings"
    case Page.Chat(chatId) => s"#chat/$chatId"

  private def parsePage(hash: String): Page =
    val h = hash.stripPrefix("#")
    h match
      case "login"                    => Page.Login
      case "register"                 => Page.Register
      case "settings"                 => Page.Settings
      case s if s.startsWith("chat/") =>
        val chatId = s.stripPrefix("chat/")
        if chatId.nonEmpty then Page.Chat(chatId) else Page.Landing
      case _ => Page.Landing
