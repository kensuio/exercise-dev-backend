package forex.services.oneforge

import forex.config.OneForgeCacheConfig
import forex.domain.Rate
import zio._

import java.time.{OffsetDateTime, ZoneOffset}

trait OneForgeCache {
  def getOrUpdate(pair: Rate.Pair, fetchUpdated: IO[OneForgeError, List[Rate]]): IO[OneForgeError, Rate]
}

final case class RefOneForgeCache(config: OneForgeCacheConfig, clock: Clock, ref: Ref[Option[CacheContent]])
  extends OneForgeCache {

  def getOrUpdate(pair: Rate.Pair, fetchUpdated: IO[OneForgeError, List[Rate]]): IO[OneForgeError, Rate] =
    ref.get.flatMap { cacheRef =>
      val now = OffsetDateTime.now(ZoneOffset.UTC)
      cacheRef match {
        case Some(CacheContent(rates, timestamp)) if timestamp.plusSeconds(config.ttl.toSeconds).isAfter(now) =>
          ZIO.succeed(rates)
        case _                                                                                                =>
          fetchUpdated.tap(rates => ref.set(Some(CacheContent(rates, now))))
      }
    }.flatMap(rates => ZIO.fromOption(rates.find(_.pair == pair)).orElseFail(OneForgeError.MissingPair(pair)))
}

object OneForgeCache {

  def live: URLayer[OneForgeCacheConfig, OneForgeCache] =
    ZLayer {
      for {
        config <- ZIO.service[OneForgeCacheConfig]
        clock  <- ZIO.clock
        ref    <- Ref.make[Option[CacheContent]](None)
      } yield RefOneForgeCache(config, clock, ref)
    }
}

final case class CacheContent(rates: List[Rate], lastUpdatedAt: OffsetDateTime)
