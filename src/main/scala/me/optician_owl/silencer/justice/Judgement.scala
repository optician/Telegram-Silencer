package me.optician_owl.silencer.justice

import me.optician_owl.silencer.RWS
import me.optician_owl.silencer.model._
import cats.syntax.semigroup._

import scala.concurrent.Future

object Judgement {
  def judge(xs: List[Evidence]): RWS[Verdict] =
    new RWS(Future.successful((msg, userStat) => {

      val facts = Facts(userStat, xs, msg.chat)

      val verdict = Rule.codex.foldLeft(Innocent: Verdict)(_ |+| _.apply(facts))

      val log = Vector(s"verdict is [${verdict.toString}]")

      val newStat = verdict match {
        case Innocent         => userStat
        case Infringement(ys) => userStat.newGuilt(msg.chat, ys.toList)
      }

      Future.successful((log, newStat, verdict))
    }))
}
