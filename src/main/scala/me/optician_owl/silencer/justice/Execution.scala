package me.optician_owl.silencer.justice

import me.optician_owl.silencer._
import me.optician_owl.silencer.model.Verdict
import cats.syntax.all._
import cats.instances.vector._
import cats.instances.future._
import cats.data.ReaderWriterStateT._

import scala.concurrent.ExecutionContext

object Execution {
  def punish(verdict: Verdict)(implicit ex: ExecutionContext): RWS[Verdict] = {
    for {
      v <- verdict.pure[RWS].tell(Vector("punishments aren't yet implemented"))
    } yield v
  }
}
