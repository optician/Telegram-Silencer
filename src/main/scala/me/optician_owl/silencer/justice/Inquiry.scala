package me.optician_owl.silencer.justice

import cats.data.RWST
import cats.instances.future._
import cats.instances.vector._
import info.mukel.telegrambot4s.models.{Message, MessageEntityType}
import me.optician_owl.silencer._
import me.optician_owl.silencer.model._
import me.optician_owl.silencer.services.storage.ChatSettingsService
import me.optician_owl.silencer.utils.Host

import scala.concurrent.{ExecutionContext, Future}

class Inquiry(chatSettingsService: ChatSettingsService) {

  implicit private val settings: ChatSettingsService = chatSettingsService

  def searchEvidences(implicit ex: ExecutionContext): MessageRWS[List[Evidence]] = {
    val procedures = List(Links(), Forwards)
    for {
      evs <- RWS.traverse(procedures)(_.result).map(_.flatten)
      _   <- RWST.tell[Future, Message, Vector[String], UserStats](Vector(s"[evidences] $evs"))
    } yield evs
  }
}

/**
  * Abstraction for supposed scalability. Facilitate traverse and per chat composition of procedures.
  */
trait InquiryProcedure {
  def result: MessageRWS[List[Evidence]]
}

object InquiryProcedure {

  /**
    *
    * @param f function from message and user statistics to list of evidences
    * @return
    */
  def lift(f: (Message, UserStats) => List[Evidence]): MessageRWS[List[Evidence]] =
    new MessageRWS(
      Future.successful(
        (msg, userStat) => {
          val xs = f(msg, userStat)
          Future.successful((Vector.empty, userStat, xs))
        }
      ))
}

/**
  * Detection of telegram and web links.
  * <ul>
  * <li>Web links are ignored if exclusion list contains them. Every chat has own exclusion list.</li>
  * <li>ToDo. Looks through chat users to distinguish user link from chat link.</li>
  * </ul>
  *
  * @param chatSettingsService Storage of per chat settings
  */
private case class Links()(implicit chatSettingsService: ChatSettingsService)
    extends InquiryProcedure {

  override def result: MessageRWS[List[Evidence]] =
    InquiryProcedure.lift((msg, userStat) =>
      msg.entities.toList.flatten.collect {
        case ent if ent.`type` == MessageEntityType.Url =>
          msg.text
            .flatMap(x => Host.fromUrl(x.substring(ent.offset, ent.offset + ent.length)))
            // ToDo UrlLink
            // ToDo use black and white lists
            .collect { case x if x.isTelegram => TelegramLink }
        case ent if ent.`type` == MessageEntityType.Mention => Some(TelegramLink)
      }.flatten)
}

/**
  * Detection of message forwards. Forwards work like a link in telegram clients.
  */
private object Forwards extends InquiryProcedure {
  override def result: MessageRWS[List[Evidence]] =
    InquiryProcedure.lift((msg, userStat) => msg.forwardFromChat.map(_ => TelegramLink).toList)
}
