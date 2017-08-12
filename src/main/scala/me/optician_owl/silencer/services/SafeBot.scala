package me.optician_owl.silencer.services

import cats.data.{State, Writer, WriterT}
import cats.kernel.Monoid
import cats.syntax.all._
import cats.instances.future._
import cats.instances.vector._
import info.mukel.telegrambot4s.api.declarative.Commands
import info.mukel.telegrambot4s.api.{ChatActions, Polling, TelegramBot}
import info.mukel.telegrambot4s.methods.{GetChatAdministrators, SendMessage}
import info.mukel.telegrambot4s.models.{Chat, ChatType, Message, User}
import me.optician_owl.silencer.model._

import scala.collection.mutable
import scala.concurrent.Future

class SafeBot extends TelegramBot with Polling with Commands with ChatActions {
  // Use 'def' or 'lazy val' for the token, using a plain 'val' may/will
  // lead to initialization order issues.
  // Fetch the token from an environment variable or untracked file.
  //      lazy val token = scala.util.Properties
  //                       .envOrNone("BOT_TOKEN")
  //                       .getOrElse(Source.fromFile("bot.token").getLines().mkString)

  // ToDO persistence and thread-safe
  private val stats: mutable.Map[Long, UserStats] =
    mutable.Map().withDefault(_ => Monoid[UserStats].empty)

  // ToDo ReaderWriterStateMonad?
  type InquiryS[A]     = State[UserStats, A]
  type Logger[A]       = Writer[Vector[String], A]
  type FutureWriter[A] = WriterT[Future, Vector[String], A]

  def papersPlz(user: User, msg: Message): (UserStats, Future[Unit]) =
    validate(stats(user.id.toLong), msg, msg.chat, user).run(stats(user.id.toLong)).value

  val validate: (UserStats, Message, Chat, User) => InquiryS[Future[Unit]] =
    (history, msg, chat, _) =>
      for {
        evs <- searchEvidences(msg).modify(_.newMsg(chat))
        verdict <- judge(Facts(history, evs, chat))
      } yield {
        if (verdict != Innocent) react(chat, msg.messageId) else Future.unit
      }

  def judge(facts: Facts): InquiryS[Verdict] = {

    Rule.codex
    .foldLeft(Innocent: Verdict) { (acc, el) =>
      logger.info(s"$acc <-> ${el(facts)}; $facts")
      acc |+| el(facts)
    }
    .pure[InquiryS]
    .flatMap {
      case Innocent =>
        logger.debug("innocent")
        Innocent.pure[InquiryS].asInstanceOf[InquiryS[Verdict]]
      case x @ Infringement(xs) =>
        logger.debug(xs.toString())
        x.pure[InquiryS].modify(_.newGuilt(facts.chat, xs.toList)).asInstanceOf[InquiryS[Verdict]]
    }
  }

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
    logger.debug(xs.toString)
    State((_, xs))
  }

  def react(chat: Chat, msgId: Long): Future[Unit] = {
    val eventualTuple  = attemptToDeleteMsg.map(_.run)
    val eventualTuple1 = notifyCourt(chat, msgId).map(_.run)
    (
      for {
        _ <- WriterT(eventualTuple)
        _ <- WriterT(eventualTuple1)
      } yield ()
      ).written.map { x =>
      logger.info(x.mkString(";"))
    }
  }

  def attemptToDeleteMsg: Future[Logger[Unit]] = {
    // ToDo implement
    Future.successful(().pure[Logger])
  }

  def notifyCourt(chat: Chat, msgId: Long): Future[Logger[Unit]] = {

    def msg(admins: Seq[String]) =
      SendMessage(chat.id,
        admins.map("@" + _).mkString(" ") + " clean time",
        replyToMessageId = Some(msgId))

    // ToDo if possible then send msg to personal chat
    (for {
      admins <- request(GetChatAdministrators(chat.id))
      alarm  <- request(msg(admins.flatMap(_.user.username)))
    } yield {
      Vector(admins.toString).tell
      Vector(alarm.toString).tell
    }).recover {
      case err: Exception => Vector(err.getLocalizedMessage).tell
    }
  }

  // ToDo Hide key
  lazy val token = "408189074:AAGtJQ7clw9eS9NES-sZxYWcA2ZTfyuULlU"

  // Todo use State
  // Todo is it possible to load group history
  // Todo add rules as some general approach
  val react: Message => Unit = {
    case m
        if m.chat.`type` == ChatType.Group || m.chat.`type` == ChatType.Supergroup && m.from.isDefined =>
      val (s, a) = papersPlz(m.from.get, m)
      logger.trace(s"message: ${m.text}")
      logger.trace(s"message: $m")
      a.foreach(_ => logger.info(s"Judgement finished. User stat - $s"))
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
