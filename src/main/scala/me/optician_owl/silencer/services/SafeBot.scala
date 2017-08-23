package me.optician_owl.silencer.services

import cats.data.ReaderWriterStateT
import cats.instances.future._
import cats.instances.vector._
import cats.syntax.all._
import com.typesafe.config.ConfigFactory
import info.mukel.telegrambot4s.api.declarative.Commands
import info.mukel.telegrambot4s.api.{ChatActions, Polling, TelegramBot}
import info.mukel.telegrambot4s.methods.{GetChatAdministrators, SendMessage}
import info.mukel.telegrambot4s.models.{ChatType, Message, MessageEntityType, User}
import me.optician_owl.silencer.model._

import scala.concurrent.Future

class SafeBot(statsService: StatsService)
    extends TelegramBot
    with Polling
    with Commands
    with ChatActions {

  type RWS[A] = ReaderWriterStateT[Future, Message, Vector[String], UserStats, A]

  def papersPlz(user: User, msg: Message): Future[UserStats] =
    validate.run(msg, statsService.stats(user.id.toLong)).map {
      case (log, state, verdict) =>
        if (!verdict.isInnocent)
          logger.info(log.mkString("; "))
        else
          logger.debug(log.mkString("; "))
        statsService.updateStats(user.id.toLong, state)
    }

  def validate: RWS[Verdict] =
    for {
      _       <- statCounter
      es      <- searchEvidences
      verdict <- judge(es)
      _       <- punish(verdict)
      _       <- notifyCourt(verdict)
    } yield verdict

  def statCounter: RWS[Unit] =
    new RWS(Future.successful((msg, userStat) => {
      Future.successful((Vector.empty, userStat.newMsg(msg.chat), ()))
    }))

  def searchEvidences: RWS[List[Evidence]] = {

    // Todo distinguish user and channel
    // Todo match telegram link via regex or look at message fields
    def telegramLinks: RWS[List[Evidence]] =
      new RWS(
        Future.successful(
          (msg, userStat) => {
            val xs: List[Evidence] =
              msg.text
                .getOrElse("")
                .split("\\s")
                .collect {
                  case x if x.startsWith("@") => TelegramLink
                }
                .toList
            Future.successful((Vector.empty, userStat, xs))
          }
        )
      )

    def urlLinks: RWS[List[Evidence]] = new RWS(
      Future.successful(
        (msg, userStat) => {
          val xs: List[Evidence] = msg.entities.toList.flatten.collect {
            case ent if ent.`type` == MessageEntityType.Url => UrlLink
          }
          Future.successful((Vector.empty, userStat, xs))
        }
      )
    )

    for {
      u <- urlLinks
      t <- telegramLinks
      evidences = t ++ u
      _ <- ReaderWriterStateT.tell[Future, Message, Vector[String], UserStats](Vector(s"[evidences] $evidences"))
    } yield evidences
  }

  def judge(xs: List[Evidence]): RWS[Verdict] =
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

  def punish(verdict: Verdict): RWS[Verdict] = {
    // ToDo implement
    for {
      v <- verdict.pure[RWS].tell(Vector("punishments aren't yet implemented"))
    } yield v
  }

  def notifyCourt(verdict: Verdict): RWS[Verdict] =
    new RWS(Future.successful((msg, userStat) => {

      def alarmMsg(admins: Seq[String]) =
        SendMessage(
          msg.chat.id,
          admins.map("@" + _).mkString(" ") + " clean time",
          replyToMessageId = Some(msg.messageId)
        )

      if (!verdict.isInnocent) {
        // ToDo if possible then send msg to personal chat
        (
          for {
            admins <- request(GetChatAdministrators(msg.chat.id))
            alarm  <- request(alarmMsg(admins.flatMap(_.user.username)))
          } yield Vector(s"court [${admins.mkString(",")}] was notified with [$alarm]")
        ).recover {
            case err: Exception =>
              Vector(s"court notification failed with [${err.getLocalizedMessage}]")
          }
          .map((_, userStat, verdict))
      } else Future.successful((Vector.empty, userStat, verdict))
    }))

  lazy val token: String = ConfigFactory.load().getString("bot-token")

  // Todo is it possible to load group history?
  // ToDo on new user join check his reputation
  // ToDo then infringement occurs in one chat take action in others where culprit is present (it depends on violation type: spammer vs cad)
  val msgProcessing: Message => Unit = {
    case m
        if m.chat.`type` == ChatType.Group || m.chat.`type` == ChatType.Supergroup && m.from.isDefined =>
      papersPlz(m.from.get, m).foreach { userStats =>
        logger.trace(s"message: ${m.text}")
        logger.trace(s"message: $m")
        logger.trace(userStats.toString)
      }
    case msg =>
  }

  onCommand("/hello") { implicit msg =>
    reply(s"Hello my friend!")
  }

  onMessage(msgProcessing)
  onEditedMessage(msgProcessing)
}
