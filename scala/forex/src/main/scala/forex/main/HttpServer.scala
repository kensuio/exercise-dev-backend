package forex.main

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import forex.config.ApiConfig
import zio._
import zio.logging.{Logger, Logging}

object HttpServer {

  type HttpServer = Has[Service]

  trait Service {
    def start: Managed[Throwable, Http.ServerBinding]
  }

  val live: ZLayer[Has[ActorSystem] with Has[ApiConfig] with Has[Route] with Logging, Nothing, HttpServer] =
    ZLayer.fromServices[ActorSystem, ApiConfig, Route, Logger[String], HttpServer.Service] { (sys, cfg, routes, log) =>
      new Service {
        implicit val system: ActorSystem = sys

        val start: Managed[Throwable, Http.ServerBinding] =
          ZManaged
            .make(
              ZIO
                .fromFuture(_ => Http().newServerAt(cfg.interface, cfg.port).bind(routes))
                .zipLeft(log.info(s"Server online at http://${cfg.interface}:${cfg.port}"))
            )(b => ZIO.fromFuture(_ => b.unbind()).orDie)
      }
    }

  def start: ZManaged[HttpServer, Throwable, Http.ServerBinding] =
    ZManaged.accessManaged[HttpServer](_.get.start)
}
