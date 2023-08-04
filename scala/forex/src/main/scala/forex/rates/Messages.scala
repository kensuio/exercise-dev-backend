package forex.rates

import forex.domain._
import scala.util.control.NoStackTrace

/* Here we define domain level entities for the rates service */

sealed trait RatesError extends Throwable with NoStackTrace

/** Domain-level errors to the rates request */
object RatesError {
  final case class Generic(description: Option[String] = None) extends RatesError {
    override def toString(): String = s"Generic error while determining rates: ${description.getOrElse("")}."
  }
  final case class ClientError(description: String) extends RatesError {
    override def toString(): String = s"Client error while determining rates: $description."
  }
  final case class CommunicationError(inner: Throwable) extends RatesError {
    override def toString(): String = s"Unspecific error in communication with backing service: ${inner.getMessage}."
  }
  final case object BackingServiceError extends RatesError {
    override def toString(): String = s"Backing service is currently malfunctioning."
  }
  final case object CouldNotDeriveRatesFromResponseError extends RatesError {
    override def toString(): String = s"Could not derive rates from backing service response."
  }
  final case class System(underlying: Throwable) extends RatesError
}

final case class GetRequest(
  from: Currency,
  to: Currency
)
