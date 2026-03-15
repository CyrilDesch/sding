package sding.client

import com.raquo.laminar.api.L.*
import org.scalajs.dom

object AuthState:
  private val tokenKey = "sding_token"

  val token: Var[Option[String]] = Var(
    Option(dom.window.localStorage.getItem(tokenKey)).filter(_.nonEmpty)
  )

  val isLoggedIn: Signal[Boolean] = token.signal.map(_.isDefined)

  def login(jwt: String): Unit =
    dom.window.localStorage.setItem(tokenKey, jwt)
    token.set(Some(jwt))

  def logout(): Unit =
    dom.window.localStorage.removeItem(tokenKey)
    token.set(None)
    Router.navigateTo(Page.Login)
