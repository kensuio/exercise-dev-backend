package forex

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import forex.config._
import forex.interfaces.api.Api
import forex.interfaces.api.rates.RatesApi
import forex.main.HttpServer
import forex.rates.Rates
import forex.services.oneforge.OneForge
import zio._
import zio.clock.Clock
import zio.config._
import zio.config.syntax._
import zio.config.typesafe.TypesafeConfig
import zio.logging.LogAnnotation
import zio.logging.Logging
import zio.logging.slf4j.Slf4jLogger

import java.util.concurrent.TimeUnit

object Main extends App {

  def run(args: List[String]): URIO[ZEnv, ExitCode] =
    HttpServer.start
      .useForever
      .provideCustomLayer(prepareEnvironment)
      .exitCode

  /**
   * Builds the needed layers to run the Http service based on
   * standard resources provided by the zio runtime: [[ZEnv]].
   *
   * Thus, the [[Clock]] service is a provided dependency
   */
  private def prepareEnvironment: RLayer[Clock, Has[HttpServer.Service]] = {

    /* Read configuration and builds the typed object */
    val configLayer = TypesafeConfig.fromDefaultLoader(ApplicationConfig.descriptor)

    /* Spawn the actor system, requires a system clock to define timeouts */
    val actorSystemLayer: RLayer[Clock, Has[ActorSystem]] = {
      val akkaStart = (cfg: AkkaConfig) => ZIO(ActorSystem(cfg.name))
      val akkaStop  = (sys: ActorSystem, cfg: AkkaConfig) =>
        ZIO.fromFuture(_ => sys.terminate())
          .timeout(cfg.exitJvmTimeout)
          .orDie

      val akkaConfigLayer = configLayer.narrow(_.akka) >>> ZLayer.fromEffect(getConfig[AkkaConfig])
      val clockLayer      = ZLayer.identity[Clock]

      (akkaConfigLayer ++ clockLayer) >>>
        ZLayer.fromServiceManaged {
          config => ZManaged.make(acquire = akkaStart(config))(release = akkaStop(_, config))
        }

    }

    /* Enriched Logging */
    val loggingLayer: ULayer[Logging] = Slf4jLogger.make { (context, message) =>
      val logFormat     = "[correlation-id = %s] %s"
      val correlationId = LogAnnotation.CorrelationId.render(context.get(LogAnnotation.CorrelationId))
      logFormat.format(correlationId, message)
    }

    /* Build the routes to serve, based on defined services */
    val routesLayer: ULayer[Has[Route]] = {
      // Here we need something more that a dummy
      val oneForge     = OneForge.dummy
      val ratesService = oneForge >>> Rates.oneForge
      val apiLayer     = ratesService >>> RatesApi.live >>> Api.live

      /* extract routes from the complete API */
      apiLayer >>> ZLayer.fromService(_.routes)
    }

    /* combine the required layers to start the server */
    (actorSystemLayer ++ configLayer.narrow(_.api) ++ routesLayer ++ loggingLayer) >>> HttpServer.live

  }
}
