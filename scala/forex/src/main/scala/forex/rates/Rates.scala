package forex.rates

import forex.domain._
import forex.services.oneforge.{OneForge, OneForgeError}
import zio._
import zio.http.Client
import zio.cache.{Cache, Lookup}
import scala.util.Sorting
import scala.collection.immutable.SortedSet
import forex.config.OptimizationsConfig
import forex.utils.cache.InMemoryCache

/** The domain-level service to get rates quotations */
trait Rates {

  def get(request: GetRequest): IO[RatesError, Rate]
}

/** Implementation based on the underlying one-forge api */
final case class OneForgeRates(oneForge: OneForge, config: OptimizationsConfig, cache: InMemoryCache[Rate.Pair, OneForgeError, Rate]) extends Rates {

  // One configurable optimization is querying and caching with a canonical order of currencies. 
  // If this optimization is configured and a request queries the inverted pair of a cached pair,
  // we will take the cached rate and return its inversion.
  val pairTransformer = (pair: Rate.Pair) => if (config.canonicalize) pair.canonicalized else pair
  val resultTransformer = (originalPair: Rate.Pair) => (transformedPair: Rate.Pair) => (rate: Rate) => {
    val isFlipped = originalPair != transformedPair
    if (isFlipped) rate.invert else rate
  }

  override def get(request: GetRequest): IO[RatesError, Rate] =
    // No need to make a request when asking for a conversion of a currency to itself
    if (request.from == request.to) 
      Rates.unity(request.from)
    else
      {
        val requestPair = Rate.Pair(request.from, request.to)
        val queryPair = pairTransformer(requestPair)
        for {
          result <- cache.get(queryPair).mapError(toDomainError)
          _ <- if (result.isEmpty) ZIO.fail(RatesError.Generic(Some("Unknown currency pair."))) else ZIO.unit
          rate = resultTransformer(requestPair)(queryPair)(result.get)
        } yield rate
      }

  private def toDomainError[T <: Throwable](t: T): RatesError = t match {
    case OneForgeError.Generic     => RatesError.Generic()
    case OneForgeError.GeneratedURLWasMalformed => RatesError.ClientError(s"Error in generation of URL for backing service.")
    case e @ OneForgeError.InvalidRequestError => RatesError.ClientError(e.toString())
    case OneForgeError.CommunicationError(inner) => RatesError.CommunicationError(inner)
    case OneForgeError.ServerError => RatesError.BackingServiceError
    case OneForgeError.ResponseParsingError => RatesError.CouldNotDeriveRatesFromResponseError
    case OneForgeError.System(err) => RatesError.System(err)
    case e: RatesError             => e
    case e                         => RatesError.System(e)
  }
}

object Rates {
  def unity(currency: Currency): IO[Nothing, Rate] = 
    for {
      offsetDateTime <- Clock.currentDateTime
    } yield Rate(Rate.Pair(currency, currency), Price(1.0), offsetDateTime)

  /** Builds a [[Rates]] based on the underlying one-forge implementation */
  val oneForge: URLayer[OneForge & OptimizationsConfig & InMemoryCache[Rate.Pair, OneForgeError, Rate], Rates] =
    ZLayer.fromFunction(OneForgeRates.apply _)
}
