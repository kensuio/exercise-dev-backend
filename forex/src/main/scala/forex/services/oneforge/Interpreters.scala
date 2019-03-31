package forex.services.oneforge

import java.time.OffsetDateTime

import forex.domain._
import monix.eval.Task
import org.atnos.eff._
import org.atnos.eff.all._
import org.atnos.eff.addon.monix.task._

import scala.concurrent.Future

object Interpreters {

  def dummy[R](
      implicit
      m1: _task[R]
  ): Algebra[Eff[R, ?]] = new Dummy[R]

  def live[R](
      implicit
      m1: _task[R]
  ): Algebra[Eff[R, ?]] = new Live[R]
}

final class Dummy[R] private[oneforge] (
    implicit
    m1: _task[R]
) extends Algebra[Eff[R, ?]] {

  override def get(
      pair: Rate.Pair
  ): Eff[R, Error Either Rate] =
    for {
      result ← fromTask(Task.now(Rate(pair, Price(BigDecimal(100)), Timestamp.now)))
    } yield Right(result)
}

final class Live[R] private[oneforge] (
    implicit
    m1: _task[R]
) extends Algebra[Eff[R, ?]] {

  override def get(
      pair: Rate.Pair
  ): Eff[R, Error Either Rate] =
    for {
      result ← fromTask(Task.deferFuture(Future.successful(Rate(pair, Price(BigDecimal(100)), Timestamp.now))))
    } yield Right(result)
}