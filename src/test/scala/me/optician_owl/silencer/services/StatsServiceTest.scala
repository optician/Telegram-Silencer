package me.optician_owl.silencer.services

import java.time.ZonedDateTime

import info.mukel.telegrambot4s.models.{Chat, ChatType}
import me.optician_owl.silencer.model.{Spam, UserChatStats, UserStats}
import me.optician_owl.silencer.services.storage.StatsService
import org.scalatest.{FlatSpec, Matchers}

class StatsServiceTest extends FlatSpec with Matchers {
  val service = new StatsService

  val chat = Chat(123L, ChatType.Group)

  private val usEmpty     = UserStats(ZonedDateTime.now, 0, Map.empty)
  private val us          = usEmpty.newMsg(chat)
  private val usWithGuilt = us.newGuilt(chat, Seq(Spam))
  private val time        = ZonedDateTime.now
  private val newcommer =
    usEmpty.copy(chatStats = Map(412L -> UserChatStats(time, 0, Map(), Some(time))))

  it should "convert UserStats back and forward" in {
    service.converter.invert(service.converter(usEmpty)) should be(usEmpty)
    service.converter.invert(service.converter(us)) should be(us)
    service.converter.invert(service.converter(us)) should not be usWithGuilt
    service.converter.invert(service.converter(us)) should not be usEmpty
    service.converter.invert(service.converter(usWithGuilt)) should be(usWithGuilt)
    service.converter.invert(service.converter(newcommer)) should be(newcommer)
  }

  it should "persist and read UserStats" in {
    service.updateStats(42L, usEmpty)
    service.updateStats(43L, us)
    service.updateStats(44L, usWithGuilt)
    service.updateStats(45L, newcommer)

    service.stats(41L) should be(UserStats(ZonedDateTime.now, 0, Map.empty))
    service.stats(42L) should be(usEmpty)
    service.stats(43L) should be(us)
    service.stats(44L) should be(usWithGuilt)
    service.stats(45L) should be(newcommer)
  }

  it should "drop UserStats" in {
    val userId = 55L
    service.stats(userId) should be(UserStats(ZonedDateTime.now, 0, Map.empty))
    service.updateStats(userId, us)
    service.stats(userId) should be(us)
    service.dropStats(userId)
    service.stats(userId) should be(UserStats(ZonedDateTime.now, 0, Map.empty))
  }
}
