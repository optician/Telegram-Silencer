package me.optician_owl.silencer.services

import java.time.ZonedDateTime

import info.mukel.telegrambot4s.models.{Chat, ChatType}
import me.optician_owl.silencer.model.{Spam, UserStats}
import org.scalatest.{FlatSpec, Matchers}

class StatsServiceTest extends FlatSpec with Matchers {
  val service = new StatsService

  val chat = Chat(123L, ChatType.Group)

  private val usEmpty     = UserStats(ZonedDateTime.now, 0, Map.empty)
  private val us          = usEmpty.newMsg(chat)
  private val usWithGuilt = us.newGuilt(chat, Seq(Spam))

  it should "convert UserStats back and forward" in {
    service.converter.invert(service.converter(usEmpty)) should be(usEmpty)
    service.converter.invert(service.converter(us)) should be(us)
    service.converter.invert(service.converter(us)) should not be usWithGuilt
    service.converter.invert(service.converter(us)) should not be usEmpty
    service.converter.invert(service.converter(usWithGuilt)) should be(usWithGuilt)
  }

  it should "persist and read UserStats" in {
    service.updateStats(42L, usEmpty)
    service.updateStats(43L, us)
    service.updateStats(44L, usWithGuilt)

    service.stats(41L) should be (UserStats(ZonedDateTime.now, 0, Map.empty))
    service.stats(42L) should be (usEmpty)
    service.stats(43L) should be (us)
    service.stats(44L) should be (usWithGuilt)
  }
}
