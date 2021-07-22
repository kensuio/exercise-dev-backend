package forex.main

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import forex.config.ApiConfig
import zio._
import zio.logging.{Logger, Logging}

/**
 * An akka-based server definition
 *
 * The service will create the bindings required for the actor system to run it.
 * The bindings will be used to shut down the service too.
 *
 * The `live` implementation will provide all the needed scaffolding, based on the
 * provided layer dependencies:
 * - ActorSystem to run the http server
 * - ApiConfig to provide host/port definitions
 * - Route to describe the served endpoints
 * - Logging to notify the server operations
 * It will also free the socket resources
 */
object HttpServer {

  trait Service {
    def start: Managed[Throwable, Http.ServerBinding]
  }

  val live: URLayer[Has[ActorSystem] with Has[ApiConfig] with Has[Route] with Logging, Has[Service]] =
    ZLayer.fromServices[ActorSystem, ApiConfig, Route, Logger[String], HttpServer.Service] { (sys, cfg, routes, log) =>
      new Service {
        implicit val system: ActorSystem = sys

        val start: Managed[Throwable, Http.ServerBinding] =
          ZManaged.make(
            ZIO
              .fromFuture(_ => Http().newServerAt(cfg.interface, cfg.port).bind(routes))
              .zipLeft(log.info(s"Server online at http://${cfg.interface}:${cfg.port}"))
          )(b => ZIO.fromFuture(_ => b.unbind()).orDie)
      }
    }

  /** Shortcut to start the service, given as a required dependency */
  def start: ZManaged[Has[Service], Throwable, Http.ServerBinding] =
    ZManaged.accessManaged[Has[Service]](_.get.start)
}
