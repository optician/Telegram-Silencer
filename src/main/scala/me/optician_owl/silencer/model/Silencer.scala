package me.optician_owl.silencer.model

import java.time.ZonedDateTime

import cats.data.State
import cats.kernel.Monoid
import cats.syntax.applicative._
import info.mukel.telegrambot4s.api.declarative.Commands
import info.mukel.telegrambot4s.api.{ChatActions, Polling, TelegramBot}
import info.mukel.telegrambot4s.models.{ChatType, Message}
import me.optician_owl.silencer.model.BorderGuard.Chat

import scala.collection.mutable

object Silencer {

  // ToDo Hide key
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

      // Todo use State
      // Todo is it possible to load group history
      // ToDo is it possible to request list of administrators
      // Todo add rules as some general approach
      val react: Message => Unit = {
        case m if m.chat.`type` == ChatType.Group || m.chat.`type` == ChatType.Supergroup =>
          m.from.foreach(u => stats += (u.id -> (stats.getOrElse(u.id, 0) + 1)))
          println(stats)
          println(m)
          println(m.text)
        case msg =>
          println(stats)
          println(msg)
          println(msg.text)
      }

      onCommand("/hello") { implicit msg =>
        reply("My token is SAFE!")
      }

      onMessage(react)
      onEditedMessage(react)
    }

    SafeBot.run()
  }
}

object BorderGuard {

  // ToDO persistence
  private val stats: mutable.Map[Long, UserStats] =
    mutable.Map().withDefault(_ => Monoid[UserStats].empty)

  case class Chat(id: Long) extends AnyVal

  case class User(id: Long) extends AnyVal

  type InquiryS[A] = State[UserStats, A]

  val validate: (UserStats, String, Chat, User) => InquiryS[Unit] =
    (history, msg, chat, user) => {
      for {
        _       <- ().pure[InquiryS].modify(_.newMsg(chat))
        evs     <- searchEvidences(msg)
        verdict <- judge(Facts(history, evs, chat))
      } yield if (verdict) react() else ()
    }

  def judge(facts: Facts): InquiryS[Boolean] =
    Rule.codex
      .foldLeft(false)((acc, el) => acc || el(facts))
      .pure[InquiryS]
      .modify(_.newGuilt(facts.chat, ???))

  // Todo distinguish user and channel
  // Todo match telegram link via regex
  // Todo match domains by domain lists
  def searchEvidences(msg: String): InquiryS[List[Evidence]] = {
    val xs: List[Evidence] =
      msg
        .split("\\s")
        .collect {
          case x if x.startsWith("@")                                   => TelegramLink
          case x if x.startsWith("http://") || x.startsWith("https://") => OuterLink
        }
        .toList

    xs.pure[InquiryS]
  }

  def react(): Unit = ???
}

trait Rule {
  def apply(facts: Facts): Boolean
}

object NoviceAndSpammer extends Rule {
  override def apply(facts: Facts): Boolean = {
    // Danger place
    // ToDo check existence of stats
    val chatStats = facts.userStats.chatStats(facts.chat)

    facts.evidences.nonEmpty &&
    chatStats.amountOfMessages <= 10 &&
    chatStats.firstAppearance.isAfter(ZonedDateTime.now().minusMonths(1))
  }
}

object Rule {
  val codex: List[Rule] = List(NoviceAndSpammer)
}

case class Facts(userStats: UserStats, evidences: List[Evidence], chat: Chat)

sealed trait Infringement

object Spam extends Infringement

sealed trait Evidence

object OuterLink extends Evidence

object TelegramLink extends Evidence

case class GuiltRecord()

case class GuiltJournal(journal: List[GuiltRecord])
