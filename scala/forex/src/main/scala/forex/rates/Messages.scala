package forex.rates

import forex.domain._

import scala.util.control.NoStackTrace

sealed trait RatesError extends Throwable with NoStackTrace

object RatesError {
  final case object Generic extends RatesError
  final case class System(underlying: Throwable) extends RatesError
}

final case class GetRequest(
  from: Currency,
  to: Currency
)
