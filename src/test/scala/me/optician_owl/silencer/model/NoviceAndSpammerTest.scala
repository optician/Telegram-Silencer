package me.optician_owl.silencer.model

import java.time.ZonedDateTime

import cats.data.NonEmptyList
import info.mukel.telegrambot4s.models.{Chat, ChatType}
import org.scalatest.{FlatSpec, Matchers}

class NoviceAndSpammerTest extends FlatSpec with Matchers {

  behavior of "NoviceAndSpammerTest"

  private val chatID = 1001L

  val facts = Facts(_: UserStats, List(UrlLink("link.com")), Chat(chatID, ChatType.Group))

  def stats(userChatStats: UserChatStats): UserStats =
    UserStats(ZonedDateTime.now, 2, Map(), Map(chatID -> userChatStats))

  def chatStats(isNewbie: Boolean): UserChatStats = {
    val time = ZonedDateTime.now
    UserChatStats(time, 2, Map(), if (isNewbie) Some(time) else None)
  }

  private val factsFabric = facts compose stats compose chatStats

  private val oldTimerFacts = factsFabric(false)
  private val newbieFacts   = factsFabric(true)

  val rule1 = new NoviceAndSpammer(2)
  val rule2 = new NoviceAndSpammer(1)

  it should "ignore old-timers and check novices" in {
    rule1(oldTimerFacts) should be(Innocent)
    rule1(newbieFacts) should be(Infringement(NonEmptyList(Spam, Nil)))
  }

  it should "ignore veterans (users who have amount of messages greater than novice boundary)" in {
    rule1(newbieFacts) should be(Infringement(NonEmptyList(Spam, Nil)))
    rule2(newbieFacts) should be(Innocent)
  }

  it should "judge using evidences" in {
    rule1(newbieFacts) should be(Infringement(NonEmptyList(Spam, Nil)))
    rule1(newbieFacts.copy(evidences = List())) should be(Innocent)
  }

}
