package forex.services.oneforge

import java.time.OffsetDateTime

import forex.domain._
import zio._
import zio.http.Client
import forex.config.OneForgeConfig
import zio.http.URL
import java.net.URI
import zio.http.Request
import zio.http.Method
import zio.json._
import forex.services.oneforge.OneForgeError

/** One-forge service specific client api */
trait OneForge {
  def get(pair: Rate.Pair): IO[OneForgeError, Rate]
  def getMultiple(pairs: Seq[Rate.Pair]): IO[OneForgeError, Seq[Rate]]
}

final case class DummyOneForge() extends OneForge {
  override def get(pair: Rate.Pair): IO[OneForgeError, Rate] =
    ZIO.succeed(Rate(pair, Price(BigDecimal(1.0)), OffsetDateTime.now()))

  override def getMultiple(keys: Seq[Rate.Pair]): IO[OneForgeError, Seq[Rate]] =
    ZIO.succeed(keys.map(pair => Rate(pair, Price(BigDecimal(1.0)), OffsetDateTime.now())))
}

// Can't easily use ZIO Dependency injection with akka http - so we use constructor injection instead.
final case class ZIOHttpOneForge(client: Client, config: OneForgeConfig) extends OneForge {

    private def buildURL(config: OneForgeConfig, pairs: Seq[Rate.Pair]): Either[Throwable, URL] = {
      val normalizedBaseUrl = config.baseUrl.stripSuffix("/")
      val pairsString = pairs.map(pair => s"${pair.from}/${pair.to}").mkString(",")
      val urlString = s"https://${normalizedBaseUrl}/quotes?pairs=$pairsString&api_key=${config.apiKey}"
      val urlOpt = URL.fromURI(new URI(urlString))
      urlOpt match {
        case Some(url) => Right(url)
        case None => Left(new Throwable(s"Invalid backing API URL [${urlString.replace(config.apiKey, "[*****]")}]. Check configuration and logic."))
      }
    }

    private def buildRequest(url: URL): Request = 
      Request.default(method = Method.GET, url = url)
  
    override def get(pair: Rate.Pair): IO[OneForgeError, Rate] =
      for {
        url <- ZIO.fromEither(buildURL(config, Seq(pair))).mapError(e => OneForgeError.GeneratedURLWasMalformed)
        request = buildRequest(url)
        res <- client.request(request).mapError(e => OneForgeError.CommunicationError(e))
        bodyString <- res.body.asString.mapError(e => OneForgeError.CommunicationError(e))
        oneForgeQuote <- ZIO.fromEither(OneForgeResponse.pairQuoteDecoder.decodeJson(bodyString))
                         .mapError(e => OneForgeError.ResponseParsingError(bodyString))
        oneForgePairQuote = oneForgeQuote
        rate = Rate(pair, Price(oneForgePairQuote.rate), oneForgePairQuote.timestamp)
      } yield rate

    override def getMultiple(pairs: Seq[Rate.Pair]): IO[OneForgeError, Seq[Rate]] =
      for {
        url <- ZIO.fromEither(buildURL(config, pairs)).mapError(e => OneForgeError.GeneratedURLWasMalformed)
        request = buildRequest(url)
        res <- client.request(request).mapError(e => OneForgeError.CommunicationError(e))
        bodyString <- res.body.asString.mapError(e => OneForgeError.CommunicationError(e))
        oneForgeQuote <- ZIO.fromEither(bodyString.fromJson[Array[OneForgeResponse.PairQuote]]).mapError(e => OneForgeError.ResponseParsingError(bodyString))
        rates <- ZIO.fromEither(OneForgeResponse.quoteToRates(oneForgeQuote.toSeq)).mapError(e => OneForgeError.ResponseParsingError(bodyString))
      } yield rates
      
}

object OneForge {

  // Dummy is not enough...
  val dummy: ULayer[OneForge] =
    ZLayer.succeed(DummyOneForge())
  
  val live: ZLayer[Client & OneForgeConfig, Nothing, ZIOHttpOneForge] =
    ZLayer.fromZIO(
      for {
        client <- ZIO.service[Client]
        config <- ZIO.service[OneForgeConfig]
      } yield ZIOHttpOneForge(client, config)
    )
}
