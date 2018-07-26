package me.optician_owl.silencer.justice

import cats.instances.future._
import cats.instances.vector._
import cats.syntax.all._
import info.mukel.telegrambot4s.methods._
import info.mukel.telegrambot4s.models._
import me.optician_owl.silencer._
import me.optician_owl.silencer.model._
import me.optician_owl.silencer.services.BotService

import scala.concurrent.{ExecutionContext, Future}

class Execution(botService: BotService)(implicit ex: ExecutionContext) {

  private val delAndBan = InlineKeyboardButton.callbackData("delete and ban", DeleteMsgAndBan.cmd)
  private val del       = InlineKeyboardButton.callbackData("delete", DeleteMsg.cmd)
  private val forgive   = InlineKeyboardButton.callbackData("forgive", Forgive.cmd)

  private val inlineKeyboardMarkup = InlineKeyboardMarkup.singleRow(Seq(delAndBan, del, forgive))

  def notifyCourt(verdict: Verdict): MessageRWS[Verdict] =
    new MessageRWS(Future.successful((msg, userStat) => {

      def alarmMsg(admins: Seq[String], isAdmin: Boolean) = {
        SendMessage(
          msg.chat.id,
          admins.map("@" + _).mkString(" ") + " clean time",
          replyToMessageId = Some(msg.messageId),
          replyMarkup = if (isAdmin) Some(inlineKeyboardMarkup) else None
        )
      }

      if (!verdict.isInnocent) {
        // ToDo if possible then send msg to personal chat
        (
          for {
            (admins, isBotAdmin) <- botService.getAdmins(msg.chat.id)
            alarm                <- botService.request(alarmMsg(admins.flatMap(_.user.username), isBotAdmin))
          } yield s"court [${admins.mkString(",")}] was notified with [$alarm]"
        ).recover {
            case err: Exception =>
              s"court notification failed with [${err.getLocalizedMessage}]"
          }
          .map(log => (Vector(log), userStat, verdict))
      } else Future.successful((Vector.empty, userStat, verdict))
    }))

  def punish(verdict: Verdict): MessageRWS[Verdict] = {
    verdict match {
      case Innocent => verdict.pure[MessageRWS]
      case Infringement(xs) if xs.exists(_ == AnnoyingSpam) =>
        new MessageRWS(Future.successful((msg: Message, userStat: UserStats) => {
          botService.request(DeleteMessage(msg.chat.id, msg.messageId))
          botService.request(RestrictChatMember(msg.chat.id, msg.from.get.id))
          Future.successful((Vector(s"Annoying Spam was deleted [$msg]"), userStat, verdict))
        }))
      case _ => notifyCourt(verdict)
    }
    for {
      v <- verdict.pure[MessageRWS]
      _ <- RWS.tellMsg("punishments aren't yet implemented")
    } yield v
  }

  def complyWithDecision: CallbackRWS[Unit] = {

    /**
      * If delete or ban is successful then empty send acknowledge
      * else send errNotification and add log record.
      *
      * If acknowledge is not delivered then log error.
      *
      * @param errNotification message for notification about error
      * @param b boolean result of ban or mesege delete
      * @return
      */
    def commandResultProcessing(errNotification: String)(b: Boolean): CallbackRWS[Unit] =
      RWS
        .ask[CallbackQuery]
        .flatMap(
          (e: CallbackQuery) =>
            if (b) RWS.liftClb(botService.ackCallback()(e))
            else
              for {
                _ <- RWS.tellClb(errNotification)
                b <- RWS.liftClb(botService.ackCallback(Some(errNotification))(e))
              } yield b)
        .flatMap(if (_) RWS.pureClb(()) else RWS.tellClb("Failed to show notification."))

    // ToDo increment culprit stats
    def deleteMsg(chat: Chat, msg: Message): CallbackRWS[Unit] =
      for {
        b <- RWS.liftClb(botService.request(DeleteMessage(chat.id, msg.messageId)))
        _ <- commandResultProcessing(s"[$msg] remove failed")(b)
        _ <- RWS.tellClb(s"[$msg] has been deleted from [$chat]")
      } yield {}

    def ban(chat: Chat, user: User): CallbackRWS[Unit] =
      for {
        b <- RWS.liftClb(botService.request(RestrictChatMember(chat.id, user.id)))
        _ <- commandResultProcessing(s"ban of [$user] in [$chat] failed")(b)
        _ <- RWS.tellClb(s"[$user] has been banned in [$chat]")
      } yield {}

    def act(query: CallbackQuery): CallbackRWS[Unit] = {
      query match {
        case CallbackQuery(_, _, Some(msg), _, _, Some(data), _) =>
          val chat = msg.chat
          // ToDo add some safety plz
          val evidenceMsg       = msg.replyToMessage.get
          val courtNotification = msg
          data match {
            case cmd if cmd == DeleteMsg.cmd =>
              for {
                _ <- RWS.traverse(List(evidenceMsg, courtNotification))(deleteMsg(chat, _))
                _ <- RWS.tellClb("court chose [delete]")
              } yield {}
            case cmd if cmd == DeleteMsgAndBan.cmd =>
              for {
                _ <- RWS.traverse(List(evidenceMsg, courtNotification))(deleteMsg(chat, _))
                _ <- ban(chat, evidenceMsg.from.get) // ToDo add some safety
                _ <- RWS.tellClb("court chose [delete and ban]")
              } yield {}
            case cmd if cmd == Forgive.cmd =>
              for {
                _ <- deleteMsg(chat, courtNotification)
                _ <- RWS.tellClb("court chose [forgive]")
              } yield {}
            case _ =>
              RWS.failedClb(s"Undefined callback command or payload $query")
          }
        case _ => RWS.failedClb(s"Unspecified callback command $query")
      }
    }

    RWS.ask[CallbackQuery].flatMap(act)
  }
}
