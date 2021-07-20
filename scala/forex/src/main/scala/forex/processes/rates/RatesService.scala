package forex.processes.rates

import forex.domain._
import forex.services.oneforge.{OneForge, OneForgeError}
import zio._

trait RatesService {

  def get(request: GetRequest): IO[RatesError, Rate]
}

final case class OneForgeRatesService(oneForge: OneForge) extends RatesService {

  override def get(request: GetRequest): IO[RatesError, Rate] =
    oneForge.get(Rate.Pair(request.from, request.to)).mapError(toProcessError)

  private def toProcessError[T <: Throwable](t: T): RatesError = t match {
    case OneForgeError.Generic     => RatesError.Generic
    case OneForgeError.System(err) => RatesError.System(err)
    case e: RatesError             => e
    case e                         => RatesError.System(e)
  }
}

object RatesService {

  val oneForge: URLayer[Has[OneForge], Has[RatesService]] =
    (OneForgeRatesService(_)).toLayer
}
