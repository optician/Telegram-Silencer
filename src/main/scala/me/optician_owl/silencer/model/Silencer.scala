package me.optician_owl.silencer.model

import info.mukel.telegrambot4s.api.TelegramBot
import me.optician_owl.silencer.services.{SafeBot, StatsService}

object Silencer {

  def main(args: Array[String]): Unit = {
    val safeBot: TelegramBot = new SafeBot(new StatsService)
    safeBot.run()
  }
}
