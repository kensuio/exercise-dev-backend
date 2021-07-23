package forex.rates

import forex.domain._
import forex.services.oneforge.{OneForge, OneForgeError}
import zio._

/** The domain-level service to get rates quotations */
trait Rates {

  def get(request: GetRequest): IO[RatesError, Rate]
}

/** Implementation based on the underlying one-forge api */
final case class OneForgeRates(oneForge: OneForge) extends Rates {

  override def get(request: GetRequest): IO[RatesError, Rate] =
    oneForge
      .get(Rate.Pair(request.from, request.to))
      .mapError(toDomainError)

  private def toDomainError[T <: Throwable](t: T): RatesError = t match {
    case OneForgeError.Generic     => RatesError.Generic
    case OneForgeError.System(err) => RatesError.System(err)
    case e: RatesError             => e
    case e                         => RatesError.System(e)
  }
}

object Rates {

  /** Builds a [[Rates]] based on the underlying one-forge implementation */
  val oneForge: URLayer[Has[OneForge], Has[Rates]] =
    (OneForgeRates(_)).toLayer
}
