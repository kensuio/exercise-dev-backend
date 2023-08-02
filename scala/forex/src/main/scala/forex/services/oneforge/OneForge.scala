package forex.services.oneforge

import forex.config.OneForgeConfig

import java.time.OffsetDateTime
import forex.domain._
import forex.services.oneforge.Protocol.OneForgeRate
import sttp.client4.{basicRequest, DeserializationException, HttpError, UriContext}
import sttp.client4.httpclient.zio.SttpClient
import sttp.client4.ziojson.asJson
import zio._

/** One-forge service specific client api */
trait OneForge {
  def get(pair: Rate.Pair): IO[OneForgeError, Rate]
}

final case class DummyOneForge() extends OneForge {

  override def get(pair: Rate.Pair): IO[OneForgeError, Rate] =
    ZIO.succeed(Rate(pair, Price(2), OffsetDateTime.now()))
}

final case class OneForgeClient(config: OneForgeConfig, backend: SttpClient, oneForgeCache: OneForgeCache)
  extends OneForge {

  override def get(pair: Rate.Pair): IO[OneForgeError, Rate] =
    for {
      rate <- oneForgeCache.getOrUpdate(pair, fetchRates)
    } yield rate

  private def fetchRates: IO[OneForgeError, List[Rate]] = {
    val allPairsQueryValue: String = Currency.currencyPairs
      .map { case (from, to) => s"$from/$to" }.mkString(",")
    val uri                        = uri"${config.endpoint}?pairs=${allPairsQueryValue}&api_key=${config.apiKey}"
    ZIO.logInfo(s"Calling One Forge API") *>
      basicRequest
        .get(uri)
        .response(asJson[List[OneForgeRate]])
        .send(backend)
        .mapError(e => OneForgeError.System(e))
        .flatMap {
          _.body match {
            case Right(oneForgeRates) =>
              ZIO.succeed(oneForgeRates.map(Converters.toRate))
            case Left(error)          => error match {
                case HttpError(body, statusCode)        =>
                  ZIO.fail(OneForgeError.Http(body, statusCode.code))
                case DeserializationException(b, error) =>
                  ZIO.fail(OneForgeError.Deserialization(b, error))
                case e                                  =>
                  ZIO.fail(OneForgeError.System(e))
              }
          }
        }
  }
}

object OneForge {

  val live: URLayer[OneForgeConfig with SttpClient with OneForgeCache, OneForge] =
    ZLayer {
      for {
        config <- ZIO.service[OneForgeConfig]
        client <- ZIO.service[SttpClient]
        cache  <- ZIO.service[OneForgeCache]
      } yield OneForgeClient(config, client, cache)
    }

  // Dummy is not enough...
  val dummy: ULayer[OneForge]                                                    =
    ZLayer.succeed(DummyOneForge())
}
