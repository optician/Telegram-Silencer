package me.optician_owl.silencer.model

import info.mukel.telegrambot4s.models.Chat

sealed trait Evidence
case class UrlLink(link: String) extends Evidence

object TelegramLink extends Evidence {
  override def toString: String = "Telegram Link"
}

object ChineseCrutch extends Evidence {
  override def toString: String = "╋VX,QQ（同号）：253239090"
}

// Union type for poor
//case class TelegramLink[T](linkTo: Link[T]) extends Evidence {
//  override def toString: String = "Telegram Link"
//}

class Link[T](val value: T) extends AnyVal
object Link {
  implicit class ChatOps(chat: Chat) {
    def toLink = new Link[Chat](chat)
  }
  implicit class UserOps(chat: Chat) {
    def toLink = new Link[Chat](chat)
  }
}
