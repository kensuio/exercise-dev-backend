package forex

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import com.typesafe.config.{Config, ConfigFactory}
import forex.config._
import forex.interfaces.api.Api
import forex.interfaces.api.rates.RatesApi
import forex.main._
import forex.main.HttpServer.HttpServer
import forex.rates.Rates
import forex.services.oneforge.OneForge
import zio._
import zio.clock.Clock
import zio.config._
import zio.config.syntax._
import zio.config.typesafe.TypesafeConfig
import zio.logging.{LogAnnotation, Logging}
import zio.logging.slf4j.Slf4jLogger

object Main extends App {

  def run(args: List[String]): URIO[ZEnv, ExitCode] =
    ZIO(ConfigFactory.load.resolve)
      .flatMap(rawConfig => HttpServer.start.useForever.provideCustomLayer(prepareEnvironment(rawConfig)))
      .exitCode

  private def prepareEnvironment(rawConfig: Config): RLayer[Clock, HttpServer] = {
    val configLayer = TypesafeConfig.fromTypesafeConfig(rawConfig, ApplicationConfig.descriptor)

    val clock = ZLayer.identity[Clock]

    val actorSystemLayer: RLayer[Clock, Has[ActorSystem]] =
      (clock ++ configLayer.narrow(_.akka)) >>> ZLayer.fromManaged {
        ZManaged.fromEffect(getConfig[AkkaConfig]).flatMap(config =>
          ZManaged.make(ZIO(ActorSystem(config.name)))(s =>
            ZIO.fromFuture(_ => s.terminate()).timeout(config.exitJvmTimeout).orDie
          )
        )
      }

    val loggingLayer: ULayer[Logging] = Slf4jLogger.make { (context, message) =>
      val logFormat     = "[correlation-id = %s] %s"
      val correlationId = LogAnnotation.CorrelationId.render(context.get(LogAnnotation.CorrelationId))
      logFormat.format(correlationId, message)
    }

    val oneForge = OneForge.dummy

    val ratesService = oneForge >>> Rates.oneForge

    val ratesApi = ratesService >>> RatesApi.live

    val apiLayer: RLayer[Clock, Has[Api]] =
      (configLayer ++ actorSystemLayer ++ loggingLayer ++ ratesApi) >>> Api.live

    val routesLayer: URLayer[Has[Api], Has[Route]] =
      ZLayer.fromService(_.routes)

    val serverEnv: RLayer[Clock, HttpServer] =
      (actorSystemLayer ++ configLayer.narrow(_.api) ++ (apiLayer >>> routesLayer) ++ loggingLayer) >>> HttpServer.live

    serverEnv
  }
}
