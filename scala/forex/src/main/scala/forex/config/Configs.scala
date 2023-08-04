package forex.config

import zio.config.magnolia.Descriptor
import zio._

/* Here we collect the definitions that represent configurations needed to
 * run the application, as composite object trees
 */

/** Overall config */
final case class ApplicationConfig(
  akka: AkkaConfig,
  api: ApiConfig,
  optimizations: OptimizationsConfig,
  oneForge: OneForgeConfig,
)

object ApplicationConfig {

  /** Provides configuration using [[zio.config]] */
  val descriptor = Descriptor.descriptor[ApplicationConfig]
}

/** Needed to run the akka system */
final case class AkkaConfig(
  name: String,
  exitJvmTimeout: Duration
)

/** Where to sun the server */
final case class ApiConfig(
  interface: String,
  port: Int
)

/** How to retry an effect */
final case class RetryConfig(
  maxRetries: Int,
  baseDelayMilliseconds: Int,
  exponentialBackoff: Boolean,
) {
  def effectWithRetry[I, E, O](effect: ZIO[I, E, O]): ZIO[I, E, O] = {
    val retrySchedule = 
      (if (exponentialBackoff) 
        Schedule.exponential(baseDelayMilliseconds.milliseconds)
      else Schedule.spaced(baseDelayMilliseconds.milliseconds)).jittered && Schedule.recurs(maxRetries)
    effect.retry(retrySchedule)
  }
}

/** Which optimizations to use */
final case class OptimizationsConfig(
  // Canonicalize the ordering of currency-pairs in backing-service queries and cache.
  // Invert the found rate if the canonically ordered pair is the flipped queried pair
  // (accepting the small error when calculating the inverse of a `BigDecimal`).
  canonicalize: Boolean,
  // Fetch all known currency pairs every time.
  // WARNING: For n known currencies, this will result in a query for 2*(n-choose-2) rates at once
  // (n-choose-2 if `canonicalize` is `true`). Only use while supported number of currencies is small.
  fetchAll: Boolean,
  // Cache the results of the queries for a given duration.
  cacheTTLMinutes: Int = 5,
  // Retry lookup with exponential backoff
  retryLookupConfig: Option[RetryConfig] = None,
  retryFetchAllConfig: Option[RetryConfig] = None,
) {
  def cacheCapacity(noOfKnownCurrencies: Int): Int = noOfKnownCurrencies * (if (canonicalize) 1 else 2)
}

/** Configuration for accessing the OneForge API */
final case class OneForgeConfig(
  apiKey: String,
  baseUrl: String = "api.1forge.com",
)

