package forex.main

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import forex.config.ApiConfig
import zio._

trait HttpServer {
  def start: ZIO[Scope, Throwable, Http.ServerBinding]
}

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
 * It will also free the socket resources
 */
object HttpServer {

  val live: URLayer[ActorSystem & ApiConfig & Route, HttpServer] =
    ZLayer.fromZIO {
      for {
        sys    <- ZIO.service[ActorSystem]
        cfg    <- ZIO.service[ApiConfig]
        routes <- ZIO.service[Route]
      } yield new HttpServer {
        implicit val system: ActorSystem = sys

        val start: ZIO[Scope, Throwable, Http.ServerBinding] =
          ZIO.acquireRelease(
            ZIO
              .fromFuture(_ => Http().newServerAt(cfg.interface, cfg.port).bind(routes))
              .zipLeft(ZIO.logInfo(s"Server online at http://${cfg.interface}:${cfg.port}"))
          )(b => ZIO.fromFuture(_ => b.unbind()).orDie)
      }
    }

  /** Shortcut to start the service, given as a required dependency */
  def start: ZIO[Scope & HttpServer, Throwable, Http.ServerBinding] =
    ZIO.serviceWithZIO[HttpServer](_.start)
}
