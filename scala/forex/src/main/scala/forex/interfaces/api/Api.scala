package forex.interfaces.api

import akka.http.scaladsl.server.{Directives, Route}
import forex.interfaces.api.utils._
import zio._

/** Provides an api definition as akka-http routes */
trait Api {
  def routes: Route
}

/**
 * The live layer will provide basic functionalities on top of any
 * custom [[Api]], given as a required dependency.
 */
object Api {

  val live: URLayer[Api, Api] =
    ZLayer.service[Api].update(RootApi.root)
}

/* provides basic error handling transparently */
private final class RootApi(innerApi: Api) extends Api with Directives {

  override def routes: Route =
    handleExceptions(ApiExceptionHandler()) {
      handleRejections(ApiRejectionHandler()) {
        innerApi.routes
      }
    }

}

private object RootApi {
  def root(innerApi: Api): Api = new RootApi(innerApi)
}
