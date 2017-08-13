package me.optician_owl.silencer.services

import cats.Id
import cats.data.{ReaderWriterStateT, State, Writer, WriterT}
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

  private val userStatsM = Monoid[UserStats]

  // ToDo ReaderWriterStateMonad?
  type RWS[A] = ReaderWriterStateT[Future, Message, Vector[String], UserStats, A]

  def papersPlease(user: User, msg: Message): Future[UserStats] =
    validate2.run(msg, stats.getOrElse(user.id.toLong, Monoid[UserStats].empty)).map {
      case (log, state, verdict) =>
        if (!verdict.isInnocent) logger.info(log.mkString("; "))
        else logger.debug(log.mkString("; "))
        stats(user.id.toLong) = state
        state
    }

  def validate2: RWS[Verdict] =
    for {
      _       <- statCounter2
      es      <- searchEvidences2
      verdict <- judge2(es)
      _       <- punish2(verdict)
      v2      <- notifyCourt2(verdict)
    } yield v2

  def statCounter2: RWS[Unit] =
    new RWS(Future.successful((msg, userStat) => {
      Future.successful((Vector.empty, userStat.newMsg(msg.chat), ()))
    }))

  // Todo distinguish user and channel
  // Todo match telegram link via regex
  // Todo match domains by domain lists
  def searchEvidences2: RWS[List[Evidence]] =
    new RWS(Future.successful((msg, userStat) => {
      val xs: List[Evidence] =
        msg.text
          .getOrElse("")
          .split("\\s")
          .collect {
            case x if x.startsWith("@")                                   => TelegramLink
            case x if x.startsWith("http://") || x.startsWith("https://") => OuterLink
          }
          .toList
      val log = Vector(s"[evidences] $xs")
      Future.successful((log, userStatsM.empty, xs))
    }))

  def judge2(xs: List[Evidence]): RWS[Verdict] =
    new RWS(Future.successful((msg, userStat) => {

      val facts = Facts(userStat, xs, msg.chat)

      val verdict = Rule.codex.foldLeft(Innocent: Verdict)(_ |+| _.apply(facts))

      val log = Vector(s"verdict is [${verdict.toString}]")

      val newStat = verdict match {
        case Innocent         => userStat
        case Infringement(ys) => userStat.newGuilt(msg.chat, ys.toList)
      }

      Future.successful((log, newStat, verdict))
    }))

  def punish2(verdict: Verdict): RWS[Verdict] = {
    // ToDo implement
    for {
      v <- verdict.pure[RWS].tell(Vector("punishments aren't yet implemented"))
    } yield v
  }

  def notifyCourt2(verdict: Verdict): RWS[Verdict] =
    new RWS(Future.successful((msg, userStat) => {

      val chat = msg.chat

      def alarmMsg(admins: Seq[String]) =
        SendMessage(
          chat.id,
          admins.map("@" + _).mkString(" ") + " clean time",
          replyToMessageId = Some(msg.messageId)
        )

      if (!verdict.isInnocent) {
        // ToDo if possible then send msg to personal chat
        (
          for {
            admins <- request(GetChatAdministrators(chat.id))
            alarm <- request(alarmMsg(admins.flatMap(_.user.username)))
          } yield {
            Vector(s"court [${admins.mkString(",")}] was notified with [$alarm]")
          }
          ).recover {
          case err: Exception =>
            Vector(s"court notification failed with [${err.getLocalizedMessage}]")
        }
        .map(log => (log, userStat, verdict))
      }
      else Future.successful((Vector.empty, userStat, verdict))
    }))

  type InquiryS[A]     = State[UserStats, A]
  type Logger[A]       = Writer[Vector[String], A]
  type FutureWriter[A] = WriterT[Future, Vector[String], A]

  def papersPlz(user: User, msg: Message): (UserStats, Future[Unit]) =
    validate(stats(user.id.toLong), msg, msg.chat).run(stats(user.id.toLong)).value

  val validate: (UserStats, Message, Chat) => InquiryS[Future[Unit]] =
    (history, msg, chat) =>
      for {
        evs     <- searchEvidences(msg).modify(_.newMsg(chat))
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
  val msgProcessing: Message => Unit = {
    case m
        if m.chat.`type` == ChatType.Group || m.chat.`type` == ChatType.Supergroup && m.from.isDefined =>
      papersPlease(m.from.get, m).foreach { userStats =>
        logger.trace(s"message: ${m.text}")
        logger.trace(s"message: $m")
        logger.trace(userStats.toString)
      }
    case msg =>
      println(msg)
      println(msg.text)
  }

  onCommand("/hello") { implicit msg =>
    reply("My token is SAFE!")
  }

  onMessage(msgProcessing)
  onEditedMessage(msgProcessing)
}
