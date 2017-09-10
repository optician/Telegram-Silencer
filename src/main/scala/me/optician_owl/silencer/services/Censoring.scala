package me.optician_owl.silencer.services

import cats.instances.future._
import cats.instances.vector._
import com.typesafe.scalalogging.StrictLogging
import info.mukel.telegrambot4s.models._
import me.optician_owl.silencer._
import me.optician_owl.silencer.justice._
import me.optician_owl.silencer.model._
import me.optician_owl.silencer.services.storage.StatsService
import me.optician_owl.silencer.utils.Utils

import scala.concurrent.{ExecutionContext, Future}

class Censoring(
    statsService: StatsService,
    inquiry: Inquiry,
    execution: Execution,
    botService: BotService
)(implicit ex: ExecutionContext)
    extends StrictLogging {

  private def papersPlz(user: User, msg: Message): Future[UserStats] = {

    val statCounter: MessageRWS[Unit] =
      new MessageRWS(Future.successful((msg, userStat) => {
        Future.successful((Vector.empty, userStat.newMsg(msg.chat), ()))
      }))

    val process =
      for {
        _       <- statCounter
        es      <- inquiry.searchEvidences
        verdict <- Judgement.judge(es)
        _       <- execution.punish(verdict)
        _       <- execution.notifyCourt(verdict)
      } yield verdict

    process.run(msg, statsService.stats(user.id.toLong)).map {
      case (log, state, verdict) =>
        if (!verdict.isInnocent)
          logger.info(log.mkString("; "))
        else
          logger.debug(log.mkString("; "))

        statsService.updateStats(user.id.toLong, state)
    }

  }

  val msgProcessing: Message => Unit = {
    // Some kind of message. Lets investigate.
    case m
        if (m.chat.`type` == ChatType.Group || m.chat.`type` == ChatType.Supergroup)
          && m.from.isDefined
          && m.newChatMembers.isEmpty =>
      papersPlz(m.from.get, m).foreach { userStats =>
        logger.trace(s"message: $m")
        logger.trace(userStats.toString)
      }
    // Create chat statistic for recently joined users
    case m
        if (m.chat.`type` == ChatType.Group || m.chat.`type` == ChatType.Supergroup)
          && m.from.isDefined
          && m.newChatMembers.isDefined =>
      m.newChatMembers.get.foreach { u =>
        val time      = Utils.zonedDateTime(m.date)
        val userStats = statsService.stats(u.id)
        if (userStats.chatStats.get(m.chat.id).isEmpty) {
          val chatStats = UserChatStats(time, 0, Map(), Some(time))
          statsService.updateStats(
            u.id,
            userStats.copy(chatStats = userStats.chatStats + (m.chat.id -> chatStats))
          )
        } else {
          // ignore rejoining to group
        }
      }
    case msg =>
  }

  private def grantedAccess(query: CallbackQuery) =
    RWS
      .liftClb(botService.getAdmins(query.message.get.chat.id)) //ToDo make safe
      .flatMap {
        case (admins, isBotAdmin) =>
          (admins.exists(_.user.id == query.from.id), isBotAdmin) match {
            case (true, true) => execution.complyWithDecision
            case (false, _) =>
              RWS.liftClb(
                botService.ackCallback(
                  Some("Attempt of unauthorized access to punishment system."))(query)
              ).map(_ => ()) //ToDo skipped check
            case (_, false) =>
              RWS.liftClb(
                botService.ackCallback(Some("Bot should be administrator to perform action."))(
                  query)
              ).map(_ => ()) //ToDo skipped check
          }
      }

  val callbackProcessing: CallbackQuery => Unit = (query: CallbackQuery) =>
    (for {
      q          <- RWS.ask[CallbackQuery]
      _          <- RWS.tellClb(s"callback query [$q]")
      _          <- grantedAccess(q)
    } yield {})
      .run(query, statsService.stats(query.message.get.replyToMessage.get.from.get.id)) //ToDo do smthng... later...
      .foreach {
        case (log, newUserStats, _) =>
          logger.info(log.mkString("; "))
        //ToDo include userId in model
//        statsService.updateStats(query, newUserStats)
    }

  botService.onCommand("/hello")(
    implicit msg =>
      botService.reply(
        s"Hello my friend! My home page is https://github.com/optician/Telegram-Silencer"))

  botService.onMessage(msgProcessing)
  botService.onEditedMessage(msgProcessing)

  botService.onCallbackQuery(callbackProcessing)

  def run(): Unit = botService.run()

}
