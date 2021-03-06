package forex.interfaces.api.rates

import akka.http.scaladsl._
import akka.http.scaladsl.server.{Directives, Route}
import forex.domain.Currency
import forex.interfaces.api.utils.ApiMarshallers
import forex.rates.Rates
import zio._

trait RatesApi {
  def routes: Route
}

final case class DefaultRatesApi(
  ratesService: Rates
) extends RatesApi
    with Directives
    with ApiMarshallers {

  import unmarshalling.Unmarshaller
  import Converters._
  import Protocol._

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

  val live: ZLayer[Has[Rates], Nothing, Has[RatesApi]] =
    (DefaultRatesApi(_)).toLayer
}
