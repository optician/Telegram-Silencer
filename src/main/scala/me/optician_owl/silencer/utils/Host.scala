package me.optician_owl.silencer.utils

import cats.data.NonEmptyList

import scala.annotation.tailrec

/**
  * Represents host by subdomain list starting from the root domain.
  * For example ''[io,github,finch]''
  */
class Host(val list: NonEmptyList[String]) extends AnyVal

object Host {
  def apply(host: String): Host =
    new Host(NonEmptyList("/", host.split('.').reverse.toList))

  implicit val hostComparision: Ordering[Host] =
    Ordering.fromLessThan { (h1, h2) =>
      @tailrec
      def go(xs: List[String], ys: List[String]): Boolean = (xs, ys) match {
        case ("*" :: xt, _)                  => false
        case (_, "*" :: yt)                  => false
        case (xh :: xt, yh :: yt) if xh < yh => true
        case (xh :: xt, yh :: yt)            => go(xt, yt)
        case (Nil, yh :: yt)                 => true
        case _                               => false
      }
      go(h1.list.toList, h2.list.toList)
    }
}
