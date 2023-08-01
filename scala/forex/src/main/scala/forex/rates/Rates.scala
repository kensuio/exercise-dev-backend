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

  // TODO: we should update this after we introduce cache layer between API service and the OneForge client
  private def toDomainError[T <: Throwable](t: T): RatesError = t match {
    case OneForgeError.Generic     => RatesError.Generic
    case OneForgeError.System(err) => RatesError.System(err)
    case e: RatesError             => e
    case e                         => RatesError.System(e)
  }
}

object Rates {

  /** Builds a [[Rates]] based on the underlying one-forge implementation */
  val oneForge: URLayer[OneForge, Rates] =
    ZLayer.fromFunction(OneForgeRates.apply _)
}
