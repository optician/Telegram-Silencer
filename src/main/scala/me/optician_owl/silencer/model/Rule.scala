package me.optician_owl.silencer.model

import java.time.ZonedDateTime

import cats.data.NonEmptyList
import cats.kernel.Monoid

trait Rule {
  def apply(facts: Facts): Verdict
}

object NoviceAndSpammer extends Rule {

  val userChatStatsMonoid: Monoid[UserChatStats] = Monoid[UserChatStats]

  override def apply(facts: Facts): Verdict = {
    val chatStats = facts.userStats.chatStats.getOrElse(facts.chat.id, userChatStatsMonoid.empty)

    if (facts.evidences.nonEmpty &&
        chatStats.amountOfMessages <= 3 &&
        chatStats.firstAppearance.isAfter(ZonedDateTime.now().minusMonths(1)))
      Infringement(NonEmptyList(Spam, Nil))
    else Innocent
  }
}

object Rule {
  val codex: List[Rule] = List(NoviceAndSpammer)
}
