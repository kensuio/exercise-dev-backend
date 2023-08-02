package services.oneforge

import forex.config.OneForgeConfig
import forex.domain.{Currency, Price, Rate}
import forex.services.oneforge.{OneForgeCache, OneForgeClient, OneForgeError}
import forex.services.oneforge.Protocol.OneForgeRate
import sttp.client4._
import sttp.client4.httpclient.zio._
import sttp.model.StatusCode
import zio._
import zio.test._

import java.time.{Instant, OffsetDateTime, ZoneOffset}

object OneForgeSpec extends ZIOSpecDefault {
  private val testRatePair = Rate.Pair(Currency.CAD, Currency.SGD)

  private val oneForgeRate = OneForgeRate(1.2345, 1.2345, 1.2345, "CAD/SGD", 1690548673301L)

  private val configLayer: ULayer[OneForgeConfig] =
    ZLayer.succeed(OneForgeConfig("https://api.1forge.com", "YOUR_API_KEY"))

  private val cacheLayer = OneForgeCacheSpec.cacheConfigLayer ++ TestClock.default >>> OneForgeCache.live

  private def buildClient(sttpClient: SttpClient) =
    (for {
      config <- ZIO.service[OneForgeConfig]
      client <- ZIO.service[SttpClient]
      cache  <- ZIO.service[OneForgeCache]
    } yield OneForgeClient(config, client, cache)).provideLayer(configLayer ++ ZLayer.succeed(sttpClient) ++ cacheLayer)

  def spec =
    suite("OneForgeClient")(
      test("get returns the correct rate for a valid pair") {
        val sttpClient: SttpClient = HttpClientZioBackend.stub.whenAnyRequest
          .thenRespond(Response.ok(Right(List(oneForgeRate))))
        for {
          client <- buildClient(sttpClient)
          result <- client.get(testRatePair)
        } yield assertTrue(result == Rate(
          Rate.Pair(testRatePair.from, testRatePair.to),
          Price(oneForgeRate.p),
          OffsetDateTime.ofInstant(Instant.ofEpochMilli(oneForgeRate.t), ZoneOffset.UTC)
        ))
      },
      test("properly return deserialization errors") {
        val sttpClient: SttpClient = HttpClientZioBackend.stub.whenAnyRequest
          .thenRespond(Response.ok("Invalid Response"))
        for {
          client <- buildClient(sttpClient)
          result <- client.get(testRatePair).flip
        } yield result match {
          case OneForgeError.Deserialization(body, _) =>
            assertTrue(body.contains("Invalid Response"))
          case _                                      => assertTrue(false)
        }
      },
      test("returns return http errors") {
        val sttpClient: SttpClient = HttpClientZioBackend.stub.whenAnyRequest
          .thenRespond(Response("Server Error", StatusCode.InternalServerError))
        for {
          client <- buildClient(sttpClient)
          result <- client.get(testRatePair).flip
        } yield assertTrue(result == OneForgeError.Http("Server Error", 500))

      }
    )

}
