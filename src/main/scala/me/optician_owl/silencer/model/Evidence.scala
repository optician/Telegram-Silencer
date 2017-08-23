package me.optician_owl.silencer.model

sealed trait Evidence
object UrlLink    extends Evidence {
  override def toString: String = "URL Link"
}
object TelegramLink extends Evidence {
  override def toString: String = "Telegram Link"
}
