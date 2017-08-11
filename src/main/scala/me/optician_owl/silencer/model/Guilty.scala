package me.optician_owl.silencer.model

import cats.data.NonEmptyList
import cats.kernel.Monoid

sealed trait Verdict
case class Infringement(xs: NonEmptyList[Guilt]) extends Verdict
object Innocent extends Verdict

object Verdict {
  implicit val verdictMonoid: Monoid[Verdict] = new Monoid[Verdict] {
    override def empty: Verdict = Innocent

    override def combine(x: Verdict, y: Verdict): Verdict = (x, y) match {
      case (Innocent, ys) => ys
      case (xs, Innocent) => xs
      case (Infringement(xs), Infringement(ys)) => Infringement(xs ++ ys.toList)
    }
  }
}

sealed trait Guilt
object Spam extends Guilt
