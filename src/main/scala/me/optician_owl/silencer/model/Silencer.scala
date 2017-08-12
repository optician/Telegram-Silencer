package me.optician_owl.silencer.model

import info.mukel.telegrambot4s.api.TelegramBot
import me.optician_owl.silencer.services.SafeBot

object Silencer {

  def main(args: Array[String]): Unit = {
    val safeBot: TelegramBot = new SafeBot
    safeBot.run()
  }
}
