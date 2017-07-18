package me.optician_owl

import info.mukel.telegrambot4s.api.{ChatActions, Polling, TelegramBot}
import info.mukel.telegrambot4s.api.declarative.Commands

import scala.io.Source

object Silencer {

  System.setProperty("BOT_TOKEN", "408189074:AAGtJQ7clw9eS9NES-sZxYWcA2ZTfyuULlU")

  def main(args: Array[String]): Unit = {
    object SafeBot extends TelegramBot with Polling with Commands with ChatActions {
      // Use 'def' or 'lazy val' for the token, using a plain 'val' may/will
      // lead to initialization order issues.
      // Fetch the token from an environment variable or untracked file.
//      lazy val token = scala.util.Properties
//                       .envOrNone("BOT_TOKEN")
//                       .getOrElse(Source.fromFile("bot.token").getLines().mkString)

      lazy val token = "408189074:AAGtJQ7clw9eS9NES-sZxYWcA2ZTfyuULlU"

      onCommand("/hello") { implicit msg =>
        reply("My token is SAFE!")
      }
      onMessage { msg =>
        println(msg)
        println(msg.text)
      }
    }

    SafeBot.run()
  }
}
