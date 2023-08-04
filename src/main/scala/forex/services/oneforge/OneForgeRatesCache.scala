package forex.services.oneforge

import forex.utils.cache.InMemoryCache
import forex.domain.{Rate, Currency}
import zio._
import forex.config.OptimizationsConfig
import forex.services.oneforge.OneForgeError
import forex.services.oneforge.OneForge
import java.time.OffsetDateTime

object OneForgeRatesCache {

    type Lookup = (Seq[Rate.Pair]) => ZIO[Any, OneForgeError, Seq[Option[Rate]]]

    def recacheAll: ZIO[InMemoryCache[Rate.Pair, OneForgeError, Rate] & OptimizationsConfig, Throwable, Unit] =
      for {
        optimizationsConfig <- ZIO.service[OptimizationsConfig]
        ratesCache <- ZIO.service[InMemoryCache[Rate.Pair, OneForgeError, Rate]]
        pairs: Seq[Rate.Pair] = 
          (
            if (optimizationsConfig.canonicalize) Currency.allKnownPairs.toSeq else Currency.allKnownPairs.flatMap(pair => Seq(pair, pair.swap))
          ).map(tuple => Rate.Pair(tuple._1, tuple._2)).toSeq
        _ <- ratesCache.cacheWithLookup(pairs)
      } yield ()

    def recacheAllEveryNMinutesIfConfigured: ZIO[InMemoryCache[Rate.Pair, OneForgeError, Rate] & OptimizationsConfig,Throwable,Unit] =
      for {
        optimizationsConfig <- ZIO.service[OptimizationsConfig]
        retryConfigOption = optimizationsConfig.retryFetchAllConfig
        recacheEffect = retryConfigOption.map(retryConfig => retryConfig.effectWithRetry(recacheAll)).getOrElse(recacheAll)
        _ <-  if (optimizationsConfig.fetchAll && optimizationsConfig.cacheTTLMinutes > 0) recacheEffect else ZIO.unit
        _ <-  if (optimizationsConfig.fetchAll && optimizationsConfig.cacheTTLMinutes > 0) recacheEffect.schedule(
                Schedule.spaced(optimizationsConfig.cacheTTLMinutes.minutes)
              ) else ZIO.unit
      } yield ()

    def make(
      capacity: Int,
      timeToLive: Option[Duration],
    ): ZIO[OneForge & OptimizationsConfig, Nothing, InMemoryCache[Rate.Pair, OneForgeError, Rate]] =
      for {
        oneForge <- ZIO.service[OneForge]
        optimizationsConfig <- ZIO.service[OptimizationsConfig]
        retryConfigOption = optimizationsConfig.retryLookupConfig
        withOptionalRetries = (effect: IO[OneForgeError, Seq[Option[Rate]]]) => 
          retryConfigOption.map(retryConfig => retryConfig.effectWithRetry(effect)).getOrElse(effect)
        lookup =  
            (pairs: Seq[Rate.Pair]) => 
              withOptionalRetries(
                oneForge
                .getMultiple(pairs)
                // Caching needs a lookup function that returns a Seq[Option[Rate]]
                // (representing the opinionated choice that lookup-functions for a cache should support 
                // distinguishing between a failure during lookup and an unknown key - at least on the type-level).
                // Thus we map returned rates to `Some`.
                .map(_.map(Some(_)))
              )
        ref <- Ref.make(Map.empty[Rate.Pair, (Rate, Option[OffsetDateTime], Option[OffsetDateTime])])
      } yield new InMemoryCache[Rate.Pair, OneForgeError, Rate](lookup, capacity, timeToLive, ref)
}
