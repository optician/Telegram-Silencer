package me.optician_owl.silencer.model

import info.mukel.telegrambot4s.api.TelegramBot
import me.optician_owl.silencer.services.{SafeBot, StatsService}

object Silencer {

  def main(args: Array[String]): Unit = {
    val safeBot: TelegramBot = new SafeBot(new StatsService)
    safeBot.run()
  }
}
// ToDo Add soft strategy - ignore existed but unknown users before gathering minimal stat.
// ToDo Add validation for new users - check history
// ToDo Add method for clearing user stats - debugging purpose