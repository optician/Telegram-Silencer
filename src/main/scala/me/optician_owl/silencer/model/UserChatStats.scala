package me.optician_owl.silencer.model

import java.time.ZonedDateTime

import cats.Monoid
import cats.instances.all._
import cats.syntax.semigroup._

case class UserChatStats(
    firstAppearance: ZonedDateTime,
    amountOfMessages: Int,
    offences: Map[Guilt, Int],
    joiningDttm: Option[ZonedDateTime]) {

  def newGuilt(xs: Seq[Guilt]): UserChatStats = this.copy(offences = offences ++ xs.map(_ -> 1))

  def newMsg: UserChatStats = this.copy(amountOfMessages = amountOfMessages + 1)
}

object UserChatStats {

  implicit val chatStatsMonoid: Monoid[UserChatStats] = new Monoid[UserChatStats] {
    override def empty: UserChatStats = UserChatStats(ZonedDateTime.now, 0, Map.empty, None)

    override def combine(x: UserChatStats, y: UserChatStats): UserChatStats =
      UserChatStats(
        if (x.firstAppearance.isBefore(y.firstAppearance)) x.firstAppearance else y.firstAppearance,
        x.amountOfMessages + y.amountOfMessages,
        x.offences |+| y.offences,
        x.joiningDttm.orElse(y.joiningDttm)
      )
  }

}
