package me.optician_owl.silencer.utils

import cats.data.NonEmptyList

import scala.annotation.tailrec

/**
  * Represents host by subdomain list starting from the root domain.
  * For example ''[io,github,finch]''
  */
class Host(val list: NonEmptyList[String]) extends AnyVal {
  override def toString: String = list.toList.mkString(".")

  def isTelegram: Boolean = Host.telegramHosts.exists(_.linkEq(this))

  def linkEq(other: Host): Boolean = other == this

}

object Host {
  def apply(host: String): Host =
    new Host(NonEmptyList("/", host.split('.').reverse.toList))

  private val hostRegex = """^(?:http://|https://)?(?:www\.)?([^/]++)""".r

  def fromUrl(url: String): Option[Host] =
    hostRegex.findFirstMatchIn(url.toLowerCase()).map(m => Host(m.group(1)))

  val telegramHosts: List[Host] =
    List(
      Host("t.me"),
      Host("telegram.me")
    )

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
