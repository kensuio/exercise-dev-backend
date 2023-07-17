package forex

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import forex.config._
import forex.interfaces.api.Api
import forex.interfaces.api.rates.RatesApi
import forex.main.HttpServer
import forex.rates.Rates
import forex.services.oneforge.OneForge
import zio.config._
import zio.config.syntax._
import zio.config.typesafe.TypesafeConfigSource
import zio.logging.backend.SLF4J
import zio.{ULayer, ZIOAppDefault, _}

object Main extends ZIOAppDefault {

  override def run =
    ZIO.scoped((HttpServer.start) *> ZIO.never)
      .provide(prepareEnvironment)

  private def prepareEnvironment: RLayer[Any, HttpServer] = {

    /* Read configuration and builds the typed object */
    val configLayer =
      ZLayer.fromZIO(read(ApplicationConfig.descriptor.from(TypesafeConfigSource.fromResourcePath))
        .mapBoth(
          err => new RuntimeException(s"Failed to load configuration: $err"),
          identity
        ))

    /* Spawn the actor system, requires a system clock to define timeouts */
    val actorSystemLayer: RLayer[Any, ActorSystem] = {
      val akkaStart = (cfg: AkkaConfig) => ZIO.attempt(ActorSystem(cfg.name))
      val akkaStop  = (sys: ActorSystem, cfg: AkkaConfig) =>
        ZIO.fromFuture(_ => sys.terminate())
          .timeout(cfg.exitJvmTimeout)
          .orDie

      val akkaConfigLayer = configLayer.narrow(_.akka) >>> ZLayer.fromZIO(getConfig[AkkaConfig])

      akkaConfigLayer >>>
        ZLayer.scoped {
          ZIO.service[AkkaConfig].flatMap(config =>
            ZIO.acquireRelease(acquire = akkaStart(config))(release = akkaStop(_, config))
          )
        }
    }

    /* Enriched Logging */
    val loggingLayer: ULayer[Unit] =
      zio.Runtime.removeDefaultLoggers ++ SLF4J.slf4j

    /* Build the routes to serve, based on defined services */
    val routesLayer: ULayer[Route] = {
      // Here we need something more that a dummy
      val oneForge     = OneForge.dummy
      val ratesService = oneForge >>> Rates.oneForge
      val apiLayer     = ratesService >>> RatesApi.live >>> Api.live

      /* extract routes from the complete API */
      apiLayer >>> ZLayer(ZIO.serviceWith[Api](_.routes))
    }

    /* combine the required layers to start the server */
    (actorSystemLayer ++ configLayer.narrow(_.api) ++ routesLayer ++ loggingLayer) >>> HttpServer.live

  }
}
