package me.optician_owl

import cats.Applicative
import cats.data.RWST
import cats.syntax.all._
import cats.instances.vector._
import cats.instances.future._
import info.mukel.telegrambot4s.models.{CallbackQuery, Message}
import me.optician_owl.silencer.model.UserStats

import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds

package object silencer {
  type MessageRWS[A]  = RWST[Future, Message, Vector[String], UserStats, A]
  type CallbackRWS[A] = RWST[Future, CallbackQuery, Vector[String], UserStats, A]

  object RWS {
    def traverse[F[_]: Applicative, A, B](list: List[A])(func: A => F[B]): F[List[B]] =
      list.foldLeft(List.empty[B].pure[F]) { (accum, item) =>
        (accum, func(item)).mapN(_ :+ _)
      }

    def failed[E, A](msg: String)(
        implicit ex: ExecutionContext): RWST[Future, E, Vector[String], UserStats, A] =
      lift[E, A](Future.failed(new Exception(msg)))

    def failedClb[A](msg: String)(
        implicit ex: ExecutionContext): RWST[Future, CallbackQuery, Vector[String], UserStats, A] =
      failed[CallbackQuery, A](msg)

    def failedMsg[A](msg: String)(
        implicit ex: ExecutionContext): RWST[Future, Message, Vector[String], UserStats, A] =
      failed[Message, A](msg)

    def pure[E, A](a: A)(
        implicit ex: ExecutionContext): RWST[Future, E, Vector[String], UserStats, A] =
      RWST.pure[Future, E, Vector[String], UserStats, A](a: A)

    def pureClb[A](a: A)(
        implicit ex: ExecutionContext): RWST[Future, CallbackQuery, Vector[String], UserStats, A] =
      pure[CallbackQuery, A](a)

    def pureMsg[A](a: A)(
        implicit ex: ExecutionContext): RWST[Future, Message, Vector[String], UserStats, A] =
      pure[Message, A](a)

    def lift[E, A](a: Future[A])(
        implicit ex: ExecutionContext): RWST[Future, E, Vector[String], UserStats, A] =
      RWST.lift[Future, E, Vector[String], UserStats, A](a: Future[A])

    def liftClb[A](a: Future[A])(
        implicit ex: ExecutionContext): RWST[Future, CallbackQuery, Vector[String], UserStats, A] =
      lift[CallbackQuery, A](a)

    def liftMsg[A](a: Future[A])(
        implicit ex: ExecutionContext): RWST[Future, Message, Vector[String], UserStats, A] =
      lift[Message, A](a)

    def tell[E](l: String)(
        implicit ex: ExecutionContext): RWST[Future, E, Vector[String], UserStats, Unit] =
      RWST.tell[Future, E, Vector[String], UserStats](Vector(l))

    def tellClb(l: String)(implicit ex: ExecutionContext)
      : RWST[Future, CallbackQuery, Vector[String], UserStats, Unit] =
      tell[CallbackQuery](l)

    def tellMsg(l: String)(
        implicit ex: ExecutionContext): RWST[Future, Message, Vector[String], UserStats, Unit] =
      tell[Message](l)

    def ask[E](implicit ex: ExecutionContext): RWST[Future, E, Vector[String], UserStats, E] =
      RWST.ask[Future, E, Vector[String], UserStats]
  }

}
