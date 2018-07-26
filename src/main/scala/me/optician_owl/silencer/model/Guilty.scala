package me.optician_owl.silencer.model

import cats.data.NonEmptyList
import cats.kernel.Monoid

sealed trait Verdict {
  def isInnocent: Boolean
}
case class Infringement(xs: NonEmptyList[Guilt]) extends Verdict {
  override val isInnocent = false

  override def toString: String = s"guilt for ${xs.toList.mkString(",")}"
}
object Innocent extends Verdict {
  override val isInnocent = true

  override def toString: String = "innocent"
}

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
case object Spam extends Guilt {
  override def toString: String = "Spam"
}
case object AnnoyingSpam extends Guilt {
  override def toString: String = "Annoying Spam"
}
