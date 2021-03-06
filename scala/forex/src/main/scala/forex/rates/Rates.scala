package forex.rates

import forex.domain._
import forex.services.oneforge.{OneForge, OneForgeError}
import zio._

trait Rates {

  def get(request: GetRequest): IO[RatesError, Rate]
}

final case class OneForgeRates(oneForge: OneForge) extends Rates {

  override def get(request: GetRequest): IO[RatesError, Rate] =
    oneForge.get(Rate.Pair(request.from, request.to)).mapError(toProcessError)

  private def toProcessError[T <: Throwable](t: T): RatesError = t match {
    case OneForgeError.Generic     => RatesError.Generic
    case OneForgeError.System(err) => RatesError.System(err)
    case e: RatesError             => e
    case e                         => RatesError.System(e)
  }
}

object Rates {

  val oneForge: URLayer[Has[OneForge], Has[Rates]] =
    (OneForgeRates(_)).toLayer
}
