package me.optician_owl.silencer.justice

import cats.data.RWST
import cats.instances.future._
import cats.instances.vector._
import info.mukel.telegrambot4s.models.{Message, MessageEntityType}
import me.optician_owl.silencer.model._
import me.optician_owl.silencer._

import scala.concurrent.{ExecutionContext, Future}

object Inquiry {
  def lift(f: (Message, UserStats) => List[Evidence]): RWS[List[Evidence]] =
    new RWS(
      Future.successful(
        (msg, userStat) => {
          val xs = f(msg, userStat)
          Future.successful((Vector.empty, userStat, xs))
        }
      ))

  val links: RWS[List[Evidence]] = lift((msg, userStat) =>
    msg.entities.toList.flatten.collect {
      case ent if ent.`type` == MessageEntityType.Url     => UrlLink
      case ent if ent.`type` == MessageEntityType.Mention => TelegramLink
    })

  val forward: RWS[List[Evidence]] = lift(
    (msg, userStat) => msg.forwardFromChat.map(_ => TelegramLink).toList)

  def searchEvidences(implicit ex: ExecutionContext): RWS[List[Evidence]] = {
    for {
      u <- links
      t <- forward
      evidences = t ++ u
      _ <- RWST.tell[Future, Message, Vector[String], UserStats](
        Vector(s"[evidences] $evidences"))
    } yield evidences
  }
}
