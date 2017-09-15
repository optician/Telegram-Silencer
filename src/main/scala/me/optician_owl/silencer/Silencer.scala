package me.optician_owl.silencer

import com.typesafe.config.ConfigFactory
import me.optician_owl.silencer.justice.{Execution, Inquiry}
import me.optician_owl.silencer.services.storage.{ChatSettingsService, StatsService}
import me.optician_owl.silencer.services.{BotService, Censoring}
import scala.concurrent.ExecutionContext.Implicits.global

object Silencer {

  def main(args: Array[String]): Unit = {

    lazy val token: String = ConfigFactory.load().getString("bot-token")
    val botService         = new BotService(token)

    val safeBot = new Censoring(new StatsService,
                                new Inquiry(new ChatSettingsService),
                                new Execution(botService),
                                botService)
    safeBot.run()
  }
}
