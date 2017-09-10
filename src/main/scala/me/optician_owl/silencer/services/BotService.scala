package me.optician_owl.silencer.services

import info.mukel.telegrambot4s.api.declarative.{Callbacks, Commands}
import info.mukel.telegrambot4s.api.{ChatActions, Polling, TelegramBot}
import info.mukel.telegrambot4s.methods.{GetChatAdministrators, GetMe}
import info.mukel.telegrambot4s.models.{ChatId, ChatMember, User}

import scala.concurrent.Future

class BotService(val token: String)
    extends TelegramBot
    with Polling
    with Commands
    with Callbacks
    with ChatActions {

  private var me: User = _

  request(GetMe).foreach { u =>
    me = u
    logger.info(s"Me - $u")
  }

  /**
    * Return list of administrators and boolean flag - is bot an administrator
    * @param chatId id of chat
    * @return (list of administrators (bot excluded), is bot admin)
    */
  def getAdmins(chatId: ChatId): Future[(Seq[ChatMember], Boolean)] =
    request(GetChatAdministrators(chatId)).map { xs =>
      logger.debug(xs.mkString(","))
      val (admins, bot) = xs.partition(_.user.id != me.id)
      (admins, bot.nonEmpty)
    }

  def isUserAdmin(chatId: ChatId, userId: Int): Future[Boolean] =
    getAdmins(chatId).map(_._1.exists(_.user.id == userId))

}
