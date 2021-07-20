package forex

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import com.typesafe.config.{Config, ConfigFactory}
import forex.config._
import forex.interfaces.api.Api
import forex.interfaces.api.rates.RatesApi
import forex.main._
import forex.main.HttpServer.HttpServer
import forex.processes.rates.RatesService
import forex.services.oneforge.OneForge
import zio._
import zio.config.syntax._
import zio.config.typesafe.TypesafeConfig
import zio.logging.{LogAnnotation, Logging}
import zio.logging.slf4j.Slf4jLogger

object Main extends App {

  def run(args: List[String]): URIO[ZEnv, ExitCode] =
    ZIO(ConfigFactory.load.resolve)
      .flatMap(rawConfig => program.provideCustomLayer(prepareEnvironment(rawConfig)))
      .exitCode

  private val program: RIO[HttpServer with ZEnv, Unit] =
    HttpServer.start.useForever

  private def prepareEnvironment(rawConfig: Config): TaskLayer[HttpServer] = {
    val configLayer = TypesafeConfig.fromTypesafeConfig(rawConfig, ApplicationConfig.descriptor)

    val actorSystemLayer: TaskLayer[Has[ActorSystem]] = ZLayer.fromManaged {
      ZManaged.make(ZIO(ActorSystem("forex-system")))(s => ZIO.fromFuture(_ => s.terminate()).either)
    }

    val loggingLayer: ULayer[Logging] = Slf4jLogger.make { (context, message) =>
      val logFormat     = "[correlation-id = %s] %s"
      val correlationId = LogAnnotation.CorrelationId.render(context.get(LogAnnotation.CorrelationId))
      logFormat.format(correlationId, message)
    }

    val oneForge = OneForge.dummy

    val ratesService = oneForge >>> RatesService.oneForge

    val ratesApi = ratesService >>> RatesApi.live

    val apiLayer: TaskLayer[Has[Api]] =
      (configLayer ++ actorSystemLayer ++ loggingLayer ++ ratesApi) >>> Api.live

    val routesLayer: URLayer[Has[Api], Has[Route]] =
      ZLayer.fromService(_.routes)

    val serverEnv: TaskLayer[HttpServer] =
      (actorSystemLayer ++ configLayer.narrow(_.api) ++ (apiLayer >>> routesLayer) ++ loggingLayer) >>> HttpServer.live

    serverEnv
  }
}
