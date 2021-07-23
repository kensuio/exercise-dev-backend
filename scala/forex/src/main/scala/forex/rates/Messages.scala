package forex.rates

import forex.domain._
import scala.util.control.NoStackTrace

/* Here we define domain level entities for the rates service */

sealed trait RatesError extends Throwable with NoStackTrace

/** Domain-level errors to the rates request */
object RatesError {
  final case object Generic extends RatesError
  final case class System(underlying: Throwable) extends RatesError
}

final case class GetRequest(
  from: Currency,
  to: Currency
)
