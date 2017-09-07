package me.optician_owl.silencer.model

import info.mukel.telegrambot4s.api.TelegramBot
import me.optician_owl.silencer.justice.Inquiry
import me.optician_owl.silencer.services.{ChatSettingsService, SafeBot, StatsService}

object Silencer {

  def main(args: Array[String]): Unit = {
    val safeBot: TelegramBot = new SafeBot(new StatsService, new Inquiry(new ChatSettingsService))
    safeBot.run()
  }
}

// ToDo Add method for clearing user stats - debugging purpose