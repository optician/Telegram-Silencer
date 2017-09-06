package me.optician_owl

import cats.data.ReaderWriterStateT
import info.mukel.telegrambot4s.models.Message
import me.optician_owl.silencer.model.UserStats

import scala.concurrent.Future

package object silencer {
  type RWS[A] = ReaderWriterStateT[Future, Message, Vector[String], UserStats, A]
}
