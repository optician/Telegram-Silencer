package me.optician_owl.silencer.model

import java.time.ZonedDateTime

import cats.data.{NonEmptyList, State, Writer, WriterT}
import cats.kernel.Monoid
import cats.instances.future._
import cats.instances.vector._
import cats.syntax.applicative._
import cats.syntax.semigroup._
import cats.syntax.writer._
import com.typesafe.scalalogging.StrictLogging
import info.mukel.telegrambot4s.api.declarative.Commands
import info.mukel.telegrambot4s.api.{BotBase, ChatActions, Polling, TelegramBot}
import info.mukel.telegrambot4s.methods.SendMessage
import info.mukel.telegrambot4s.models.{Chat, ChatType, Message, User}

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object Silencer {

  def main(args: Array[String]): Unit = {
    val safeBot: TelegramBot = new SafeBot
    safeBot.run()
  }
}

class BorderGuard(telegramService: BotBase) extends StrictLogging {

  // ToDO persistence and thread-safe
  private val stats: mutable.Map[Long, UserStats] =
    mutable.Map().withDefault(_ => Monoid[UserStats].empty)

  // ToDo ReaderWriterStateMonad?
  type InquiryS[A]     = State[UserStats, A]
  type Logger[A]       = Writer[Vector[String], A]
  type FutureWriter[A] = WriterT[Future, Vector[String], A]

  val validate: (UserStats, Message, Chat, User) => InquiryS[Unit] =
    (history, msg, chat, user) => {
      for {
        evs     <- searchEvidences(msg).modify(_.newMsg(chat))
        verdict <- judge(Facts(history, evs, chat))
      } yield if (verdict == Innocent) react(chat, msg.messageId) else Future.unit
    }

  def judge(facts: Facts): InquiryS[Verdict] =
    Rule.codex
    .foldLeft(Innocent: Verdict)((acc, el) => acc |+| el(facts))
    .pure[InquiryS]
    .flatMap(guilts => .newGuilt(facts.chat, ???))

  // Todo distinguish user and channel
  // Todo match telegram link via regex
  // Todo match domains by domain lists
  def searchEvidences(msg: Message): InquiryS[List[Evidence]] = {
    val xs: List[Evidence] =
      msg.text
        .getOrElse("")
        .split("\\s")
        .collect {
          case x if x.startsWith("@")                                   => TelegramLink
          case x if x.startsWith("http://") || x.startsWith("https://") => OuterLink
        }
        .toList

    xs.pure[InquiryS]
  }

  def react(chat: Chat, msgId: Long): Future[Unit] =
    (for {
      _ <- WriterT(attemptToDeleteMsg.map(_.run))
      _ <- WriterT(notifyCourt(chat, msgId).map(_.run))
    } yield ()).run.map { x =>
      logger.info(x._1.mkString(";"))
      x._2
    }

  def attemptToDeleteMsg: Future[Logger[Unit]] = {
    // ToDo implement
    Future.successful(().pure[Logger])
  }

  def notifyCourt(chat: Chat, msgId: Long): Future[Logger[Unit]] = {
    // ToDo get admin list
    // ToDo if possible then send msg to personal chat
    val msg = SendMessage(chat.id, "@optician_owl clean time", replyToMessageId = Some(msgId))

    telegramService
      .request(msg)
      .map(x => Vector(x.toString).tell)
      .recover {
        case err: Exception => Vector(err.getLocalizedMessage).tell
      }
  }
}

class SafeBot extends TelegramBot with Polling with Commands with ChatActions {
  // Use 'def' or 'lazy val' for the token, using a plain 'val' may/will
  // lead to initialization order issues.
  // Fetch the token from an environment variable or untracked file.
  //      lazy val token = scala.util.Properties
  //                       .envOrNone("BOT_TOKEN")
  //                       .getOrElse(Source.fromFile("bot.token").getLines().mkString)

  // ToDo Hide key
  lazy val token = "408189074:AAGtJQ7clw9eS9NES-sZxYWcA2ZTfyuULlU"

  // Todo use State
  // Todo is it possible to load group history
  // ToDo is it possible to request list of administrators
  // Todo add rules as some general approach
  val react: Message => Unit = {
    case m if m.chat.`type` == ChatType.Group || m.chat.`type` == ChatType.Supergroup =>
      println(m)
      println(m.text)
    case msg =>
      println(msg)
      println(msg.text)
  }

  onCommand("/hello") { implicit msg =>
    reply("My token is SAFE!")
  }

  onMessage(react)
  onEditedMessage(react)
}

trait Rule {
  def apply(facts: Facts): Verdict
}

object NoviceAndSpammer extends Rule {
  override def apply(facts: Facts): Verdict = {
    // Danger place
    // ToDo check existence of stats
    val chatStats = facts.userStats.chatStats(facts.chat.id)

    if (facts.evidences.nonEmpty &&
        chatStats.amountOfMessages <= 10 &&
        chatStats.firstAppearance.isAfter(ZonedDateTime.now().minusMonths(1)))
      Infringement(NonEmptyList(Spam, Nil))
    else Innocent
  }
}

object Rule {
  val codex: List[Rule] = List(NoviceAndSpammer)
}

case class Facts(userStats: UserStats, evidences: List[Evidence], chat: Chat)

sealed trait Evidence
object OuterLink    extends Evidence
object TelegramLink extends Evidence

//case class GuiltRecord()
//
//case class GuiltJournal(journal: List[GuiltRecord])
