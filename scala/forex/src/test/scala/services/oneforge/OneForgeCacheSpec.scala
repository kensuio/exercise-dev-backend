package services.oneforge

import forex.config.OneForgeCacheConfig
import forex.domain.Currency._
import forex.domain.{Price, Rate}
import forex.services.oneforge.{CacheContent, OneForgeError, RefOneForgeCache}
import zio._
import zio.test._

import java.time.OffsetDateTime

object OneForgeCacheSpec extends ZIOSpecDefault {

  private val pair: Rate.Pair = Rate.Pair(EUR, USD)
  private val rate: Rate = Rate(pair, Price(1.2345), OffsetDateTime.now())

  private def fetchUpdatedRates: IO[OneForgeError, List[Rate]] =
    ZIO.succeed(List(rate))

  private val cacheConfig: OneForgeCacheConfig = OneForgeCacheConfig(ttl = 5.minutes)

  val cacheConfigLayer: ULayer[OneForgeCacheConfig] = ZLayer.succeed(cacheConfig)

  def buildCache(ref: Ref[Option[CacheContent]]) =
    (for {
      config <- ZIO.service[OneForgeCacheConfig]
      clock <- ZIO.service[Clock]
    } yield RefOneForgeCache(config, clock, ref)
      ).provideLayer(TestClock.default ++ ZLayer.succeed(cacheConfig))

  def spec =
    suite("OneForgeCacheSpec")(
      test("returns the cached rate if found") {
        for {
          ref <- Ref.make[Option[CacheContent]](Some(CacheContent(List(rate), OffsetDateTime.now())))
          cache <- buildCache(ref)
          result <- cache.getOrUpdate(pair, fetchUpdatedRates)
        } yield assertTrue(result == rate)
      },
      test("fetches updated rates if cache is empty") {
        for {
          ref <- Ref.make[Option[CacheContent]](None)
          cache <- buildCache(ref)
          result <- cache.getOrUpdate(pair, fetchUpdatedRates)
        } yield assertTrue(result == rate)
      },
      test("fetches updated rates if cache has expired") {
        val expiredTimestamp = OffsetDateTime.now().minusSeconds(cacheConfig.ttl.toSeconds + 1)
        for {
          ref <- Ref.make[Option[CacheContent]](Some(CacheContent(List(rate), expiredTimestamp)))
          cache <- buildCache(ref)
          result <- cache.getOrUpdate(pair, fetchUpdatedRates)
        } yield assertTrue(result == rate)
      },
      test("returns MissingPair error if rate not found in cache") {
        for {
          ref <- Ref.make[Option[CacheContent]](Some(CacheContent(List(rate), OffsetDateTime.now())))
          cache <- buildCache(ref)
          invalidPair = Rate.Pair(GBP, EUR) // Invalid pair, not in the cache
          result <- cache.getOrUpdate(invalidPair, fetchUpdatedRates).flip
        } yield assertTrue(result == OneForgeError.MissingPair(invalidPair))
      }
    )
}
