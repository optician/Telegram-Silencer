package me.optician_owl

import info.mukel.telegrambot4s.api.{ChatActions, Polling, TelegramBot}
import info.mukel.telegrambot4s.api.declarative.Commands
import info.mukel.telegrambot4s.models.{ChatType, Message}

import scala.collection.mutable
import scala.io.Source

object Silencer {

  // ToDo Hide key
  System.setProperty("BOT_TOKEN", "408189074:AAGtJQ7clw9eS9NES-sZxYWcA2ZTfyuULlU")

  // ToDO persistence
  private val stats: mutable.Map[Int, Int] = mutable.Map()

  def main(args: Array[String]): Unit = {
    object SafeBot extends TelegramBot with Polling with Commands with ChatActions {
      // Use 'def' or 'lazy val' for the token, using a plain 'val' may/will
      // lead to initialization order issues.
      // Fetch the token from an environment variable or untracked file.
//      lazy val token = scala.util.Properties
//                       .envOrNone("BOT_TOKEN")
//                       .getOrElse(Source.fromFile("bot.token").getLines().mkString)

      lazy val token = "408189074:AAGtJQ7clw9eS9NES-sZxYWcA2ZTfyuULlU"

      // Todo use State
      // Todo is it possible to load group history
      // ToDo is it possible to request list of administrators
      // Todo add rules as some general approach
      val react: Message => Unit = msg =>
        if (msg.chat.`type` == ChatType.Group || msg.chat.`type` == ChatType.Supergroup) {
          msg.from.foreach(u => stats += (u.id -> (stats.getOrElse(u.id, 0) + 1)))
          println(stats)
          println(msg)
          println(msg.text)
        }

      onCommand("/hello") { implicit msg =>
        reply("My token is SAFE!")
      }

      onMessage (react)
      onEditedMessage (react)
    }

    SafeBot.run()
  }
}
