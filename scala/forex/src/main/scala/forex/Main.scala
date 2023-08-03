package forex

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import forex.config._
import forex.domain._
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
import zio.http.Client
import forex.services.oneforge.OneForgeError
import forex.services.oneforge.ZIOHttpOneForge
import forex.utils.cache.InMemoryCache
import forex.services.oneforge.OneForgeRatesCache

object Main extends ZIOAppDefault {

  override def run =
    ZIO.scoped(
      ((HttpServer.start) *> ZIO.never) &> OneForgeRatesCache.recacheAllEveryNMinutesIfConfigured
    ).provide(prepareEnvironment)
    

  private def prepareEnvironment: ZLayer[Any, Throwable, HttpServer with InMemoryCache[Rate.Pair, OneForgeError, Rate] with OptimizationsConfig] = {

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

    val oneForgeLayer: Layer[Throwable, OneForge] = {
      val oneForgeConfigLayer = configLayer.narrow(_.oneForge) >>> ZLayer.fromZIO(getConfig[OneForgeConfig])
      (Client.default ++ oneForgeConfigLayer) >>> OneForge.live
    }

    val optimizationsConfigLayer = configLayer.narrow(_.optimizations) >>> ZLayer.fromZIO(getConfig[OptimizationsConfig])

    val ratesCacheLayer: Layer[Throwable, InMemoryCache[Rate.Pair, OneForgeError, Rate]] = 
      (oneForgeLayer ++ optimizationsConfigLayer) >>> ZLayer.fromZIO(
        for {
          optimizationConfig <- ZIO.service[OptimizationsConfig]
          oneForge <- ZIO.service[OneForge]
          cacheCapacity = optimizationConfig.cacheCapacity(Currency.allKnownPairs.size)
          cache <- OneForgeRatesCache.make(cacheCapacity, Some(optimizationConfig.cacheTTLMinutes.minutes))
        } yield cache
      )

    /* Build the routes to serve, based on defined services */
    val routesLayer: Layer[Throwable, Route] = 
      (oneForgeLayer ++ optimizationsConfigLayer ++ ratesCacheLayer) >>> ({
        Rates.oneForge >>> RatesApi.live >>> Api.live >>>  ZLayer(ZIO.serviceWith[Api](_.routes))
      })

    val apiConfigLayer = configLayer.narrow(_.api)

    /* combine the required layers to start the server and run the background-caching effect if configured */
    ((actorSystemLayer ++ apiConfigLayer ++ routesLayer) >>> HttpServer.live) ++ optimizationsConfigLayer ++ ratesCacheLayer
  }
}
