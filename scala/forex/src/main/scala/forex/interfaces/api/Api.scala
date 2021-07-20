package forex.interfaces.api

import akka.http.scaladsl.server.{ Directives, Route }
import forex.interfaces.api.rates.RatesApi
import forex.interfaces.api.utils._
import zio._

trait Api {
  def routes: Route
}

final case class RootApi(ratesApi: RatesApi) extends Api with Directives {

  override def routes: Route =
    handleExceptions(ApiExceptionHandler()) {
      handleRejections(ApiRejectionHandler()) {
        ratesApi.routes
      }
    }
}

object Api {

  val live: URLayer[Has[RatesApi], Has[Api]] =
    (RootApi(_)).toLayer
}
