package me.optician_owl.silencer.justice

import cats.data.RWST
import cats.instances.future._
import cats.instances.vector._
import info.mukel.telegrambot4s.models.{Message, MessageEntityType}
import me.optician_owl.silencer._
import me.optician_owl.silencer.model._
import me.optician_owl.silencer.services.ChatSettingsService
import me.optician_owl.silencer.utils.Host

import scala.concurrent.{ExecutionContext, Future}

class Inquiry(chatSettingsService: ChatSettingsService) {

  implicit private val settings: ChatSettingsService = chatSettingsService

  def searchEvidences(implicit ex: ExecutionContext): RWS[List[Evidence]] = {
    for {
      u <- (new Links).result
      t <- Forwards.result
      evidences = t ++ u
      _ <- RWST.tell[Future, Message, Vector[String], UserStats](Vector(s"[evidences] $evidences"))
    } yield evidences
  }
}

/**
  * Abstraction for supposed scalability. Facilitate traverse and per chat composition of procedures.
  */
trait InquiryProcedure {
  def result: RWS[List[Evidence]]
}

object InquiryProcedure {
  /**
    *
    * @param f function from message and user statistics to list of evidences
    * @return
    */
  def lift(f: (Message, UserStats) => List[Evidence]): RWS[List[Evidence]] =
    new RWS(
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
private class Links(implicit chatSettingsService: ChatSettingsService) extends InquiryProcedure {

  private val hostRegex = """^(?:http://|https://)?(?:www\.)?([^/]++)""".r
  // Performance ololo
  private def filterLinks(exclusionList: List[Host])(urlLink: UrlLink): Boolean = {
    val hostOpt = hostRegex.findFirstMatchIn(urlLink.link.toLowerCase()).map(_.group(1))
    hostOpt.fold(false)(host => exclusionList.exists(linkEq(Host(host))))
  }

  private def linkEq(host: Host)(exclusion: Host): Boolean = {
    host == exclusion
  }

  override def result: RWS[List[Evidence]] =
    InquiryProcedure.lift((msg, userStat) =>
      msg.entities.toList.flatten.collect {
        case ent if ent.`type` == MessageEntityType.Url =>
          msg.text
            .map(x => UrlLink(x.substring(ent.offset, ent.length)))
            .filter(filterLinks(chatSettingsService.getExclusionUrls(msg.chat.id)))
        case ent if ent.`type` == MessageEntityType.Mention => Some(TelegramLink)
      }.flatten)
}

/**
  * Detection of message forwards. Forwards work like a link in telegram clients.
  */
private object Forwards extends InquiryProcedure {
  override def result: RWS[List[Evidence]] = InquiryProcedure.lift(
    (msg, userStat) => msg.forwardFromChat.map(_ => TelegramLink).toList)
}