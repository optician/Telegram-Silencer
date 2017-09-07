package me.optician_owl

import cats.Applicative
import cats.data.ReaderWriterStateT
import cats.syntax.all._
import info.mukel.telegrambot4s.models.Message
import me.optician_owl.silencer.model.UserStats

import scala.concurrent.Future
import scala.language.higherKinds

package object silencer {
  type RWS[A] = ReaderWriterStateT[Future, Message, Vector[String], UserStats, A]

  object RWS {
    def traverse[F[_]: Applicative, A, B](list: List[A])(func: A => F[B]): F[List[B]] =
      list.foldLeft(List.empty[B].pure[F]) { (accum, item) =>
        (accum, func(item)).mapN(_ :+ _)
      }
  }

}
