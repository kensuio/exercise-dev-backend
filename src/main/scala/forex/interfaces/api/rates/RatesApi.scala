package forex.interfaces.api.rates

import akka.http.scaladsl._
import akka.http.scaladsl.server.{Directives, Route}
import forex.domain.Currency
import forex.interfaces.api.Api
import forex.interfaces.api.utils.ApiMarshallers
import forex.rates.Rates
import zio._

/* Does basic conversions and internal calls to the underlying service */
private final class DefaultRatesApi(
  ratesService: Rates,
) extends Api
    with Directives
    with ApiMarshallers {

  import unmarshalling.Unmarshaller
  import Converters._
  import Protocol._

  /* Akka/Future based implementation of the http endpoints.
   * No automatic typed error propagation or recovery is done here
   * like it  usually happens with [[zio.ZIO]] based services
   */

  private val currency =
    Unmarshaller.strict[String, Currency](Currency.fromString)

  override def routes: Route =
    get {
      getApiRequest { req =>
        complete {
          ratesService
            .get(toGetRequest(req))
            .map(result => toGetApiResponse(result))
        }
      }
    }

  private def getApiRequest: server.Directive1[GetApiRequest] =
    for {
      from <- parameter(Symbol("from").as(currency))
      to   <- parameter(Symbol("to").as(currency))
    } yield GetApiRequest(from, to)
}

object RatesApi {

  /**
   * Provides a [[Rates]] service exposed as an [[Api]]
   * That is, expose the service as http endpoints for akka.
   */
  val live: URLayer[Rates, Api] =
    ZLayer.fromZIO {
      for {
        rates <- ZIO.service[Rates]
      } yield new DefaultRatesApi(rates)
    }
}
