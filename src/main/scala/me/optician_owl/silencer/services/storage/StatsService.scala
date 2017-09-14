package me.optician_owl.silencer.services.storage

import java.time.{Instant, ZoneId, ZonedDateTime => ZDT}

import cats._
import com.twitter.bijection.Conversion.asMethod
import com.twitter.bijection._
import com.typesafe.scalalogging.StrictLogging
import me.optician_owl.protobuf.UserStatsScheme.{GuiltPair, UserChatStatsScheme, UserStatsScheme, Guilt => ProtoGuilt, ZonedDateTime => ProtoZDT}
import me.optician_owl.silencer.model.{Guilt, Spam, UserChatStats, UserStats}
import net.openhft.chronicle.map.{ChronicleMap, ChronicleMapBuilder}

import scala.reflect.io.{File, Path}

class StatsService extends StrictLogging {

  implicit private val zdt2zdt: Bijection[ZDT, ProtoZDT] = Bijection.build { (zdt: ZDT) =>
    ProtoZDT(zdt.toInstant.toEpochMilli, zdt.getZone.getId)
  } { myZdt =>
    ZDT.ofInstant(Instant.ofEpochMilli(myZdt.timestamp), ZoneId.of(myZdt.zone))
  }

  private val g2pg: Guilt => ProtoGuilt = {
    case Spam => ProtoGuilt.SPAM
  }

  implicit private val guilt2guilt: Bijection[Guilt, ProtoGuilt] =
    Bijection.build(g2pg) {
      case pg if pg.isSpam => Spam
    }

  implicit private val guilt2guiltPair: Bijection[(Guilt, Int), GuiltPair] =
    Bijection.build { (tuple: (Guilt, Int)) =>
      GuiltPair(tuple._1.as[ProtoGuilt], tuple._2)
    } { pair =>
      pair.guilt.as[Guilt] -> pair.amount
    }

  implicit private val ucs2ucs: Bijection[UserChatStats, UserChatStatsScheme] =
    Bijection.build { (ucs: UserChatStats) =>
      UserChatStatsScheme(ucs.firstAppearance.as[ProtoZDT],
                          ucs.amountOfMessages,
                          ucs.offences.map(_.as[GuiltPair]).toSeq,
                          ucs.joiningDttm.map(_.as[ProtoZDT]))
    } { (ucss: UserChatStatsScheme) =>
      UserChatStats(
        ucss.firstAppearance.as[ZDT],
        ucss.amountOfMessages,
        ucss.offences.map(_.as[(Guilt, Int)]).toMap,
        ucss.joiningDttm.map(_.as[ZDT])
      )
    }

  implicit private val protoUserStatsScheme: Bijection[UserStatsScheme, Array[Byte]] =
    Bijection.build { (uss: UserStatsScheme) =>
      uss.toByteArray
    }(UserStatsScheme.parseFrom)

  implicit private val proto2UserStats: Bijection[UserStats, UserStatsScheme] =
    Bijection.build { (us: UserStats) =>
      UserStatsScheme(us.firstAppearance.as[ProtoZDT],
                      us.amountOfMessages,
                      us.offences.map(_.as[GuiltPair]).toSeq,
                      us.chatStats.mapValues(_.as[UserChatStatsScheme]))
    } { uss =>
      UserStats(uss.firstAppearance.as[ZDT],
                uss.amountOfMessages,
                uss.offences.map(_.as[(Guilt, Int)]).toMap,
                uss.chatStats.mapValues(_.as[UserChatStats]))
    }

  val converter: Bijection[UserStats, Array[Byte]] =
    Bijection.connect[UserStats, UserStatsScheme, Array[Byte]]

  private val userStatsM = Monoid[UserStats]

  private val userStatsDBBuilder: ChronicleMapBuilder[Array[Byte], Array[Byte]] =
    ChronicleMapBuilder
      .of(classOf[Array[Byte]], classOf[Array[Byte]])
      .name("user-statistics")
      .entries(2e6.toInt) // Just a guess
      .averageKey(Array.fill(8)(1.toByte))
      .averageValue(converter(userStatsM.empty))

  val userStatsStore: ChronicleMap[Array[Byte], Array[Byte]] =
    userStatsDBBuilder.createPersistedTo(File(Path("db/user-stats-store.db")).jfile)

  def stats(userId: Long): UserStats =
    Option(userStatsStore.get(BigInt(userId).toByteArray))
      .map(xs => converter.invert(xs))
      .getOrElse(userStatsM.empty)

  def updateStats(userId: Long, userStats: UserStats): UserStats = {
    userStatsStore.put(BigInt(userId).toByteArray, converter(userStats))
    userStats
  }

  def dropStats(userId: Long): Unit = {
    userStatsStore.remove(BigInt(userId).toByteArray)
  }

}
