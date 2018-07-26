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
    val procedures = List(Links(), Forwards, ChineseNameSpam)
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
        //ToDo check link is not a member of a chat
        case ent if ent.`type` == MessageEntityType.Mention => Some(TelegramLink)
      }.flatten)
}

/**
  * Detection of message forwards. Forwards work like a link in telegram clients.
  */
private object Forwards extends InquiryProcedure {
  // ToDo check that link is foreigner and not from white list
  override def result: MessageRWS[List[Evidence]] =
    InquiryProcedure.lift((msg, _) => msg.forwardFromChat.map(_ => TelegramLink).toList)
}

//╋VX,QQ（同号）：253239090 专业工作室推广拉人【电报群拉国内外有无username都可拉、指定群拉人】【机器人定制】【社群代运营】【twitter关注、转发】【facebook关注、转发】【youtube点赞、评论】【出售成品电报账号】 （欢迎社群运营者、项目方、交易所洽谈合作）优质空投分享QQ群473157472 本工作室全网最低价、服务最好、活人质量最高 招收代理 We can ADD 1000+ 10000+ or ANY NUMBER REAL and ACTIVE MEMBERS for your TELEGRAM GROUPS-LEAVE NO JOIN ALERTS,QUALITY and QUANTITY GUARANTEED,DEMO AVAILABLE.We also provide READY-MADE TELEGRAM ACCOUNTS and BROADCASTING SERVICE now you read.(To get our sevic joined the group
private object ChineseNameSpam extends InquiryProcedure {
  val namePart = "╋VX,QQ（同号）"
  override def result: MessageRWS[List[Evidence]] =
    InquiryProcedure.lift((msg, _) =>
      msg.newChatMembers.toList.flatten.collect {
        case x if (x.firstName.length > 100 || x.lastName.exists(_.length > 100)) && (x.firstName.startsWith(namePart) || x.lastName.exists(_.startsWith(namePart))) =>
          ChineseCrutch
    })
}
