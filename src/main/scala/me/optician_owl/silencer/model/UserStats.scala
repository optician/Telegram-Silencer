package me.optician_owl.silencer.model

import java.time.ZonedDateTime

import cats.Monoid
import cats.syntax.semigroup._
import cats.instances.all._
import info.mukel.telegrambot4s.models.Chat

case class UserStats(
    firstAppearance: ZonedDateTime,
    amountOfMessages: Int,
    offences: Map[Guilt, Int],
    chatStats: Map[Long, UserChatStats] = Map.empty.withDefault(_ => Monoid[UserChatStats].empty)) {

  def newMsg(chat: Chat): UserStats =
    this.copy(amountOfMessages = amountOfMessages + 1,
              chatStats = chatStats + (chat.id -> chatStats(chat.id).newMsg))

  def newGuilt(chat: Chat, xs: Seq[Guilt]): UserStats = this.copy(
    offences = offences ++ xs.map(_ -> 1),
    chatStats = chatStats + (chat.id -> chatStats(chat.id).newGuilt(xs))
  )
}

object UserStats {

  implicit val statMonoid: Monoid[UserStats] = new Monoid[UserStats] {
    override def empty: UserStats = UserStats(ZonedDateTime.now, 0, Map.empty, Map.empty)

    override def combine(x: UserStats, y: UserStats): UserStats =
      UserStats(
        if (x.firstAppearance.isBefore(y.firstAppearance)) x.firstAppearance else y.firstAppearance,
        x.amountOfMessages + y.amountOfMessages,
        x.offences |+| y.offences,
        x.chatStats |+| y.chatStats
      )
  }

}
