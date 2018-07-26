package me.optician_owl.silencer.model

import cats.data.NonEmptyList
import cats.kernel.Monoid

trait Rule {
  def apply(facts: Facts): Verdict
}

class NoviceAndSpammer(noviceBoundary: Int) extends Rule {

  // ToDo Wrong responsibility. Rule shouldn't change statistics.
  private val userChatStatsMonoid: Monoid[UserChatStats] = Monoid[UserChatStats]

  override def apply(facts: Facts): Verdict = {
    val chatStats = facts.userStats.chatStats.getOrElse(facts.chat.id, userChatStatsMonoid.empty)

    if (chatStats.joiningDttm.isDefined
        && chatStats.amountOfMessages <= noviceBoundary
        && facts.evidences.nonEmpty)
      Infringement(NonEmptyList.one(Spam))
    else Innocent
  }
}

object ChineseSpammer extends Rule {
  override def apply(facts: Facts): Verdict =
    if (facts.evidences.exists(_ == ChineseCrutch)) Infringement(NonEmptyList.one(AnnoyingSpam))
    else Innocent
}

object Rule {
  val codex: List[Rule] = List(new NoviceAndSpammer(3), ChineseSpammer)
}
