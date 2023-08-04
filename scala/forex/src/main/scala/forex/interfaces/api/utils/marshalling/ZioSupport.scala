package forex.interfaces.api.utils.marshalling

import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.model._
import forex.rates.RatesError
import zio.{IO, Unsafe, ZIO}

trait ZioSupport {

  implicit val zioSupportErrorMarshaller: Marshaller[RatesError, HttpResponse] =
    Marshaller { implicit ec => error =>
      PredefinedToResponseMarshallers.fromResponse(
        error match {
          case RatesError.Generic            =>
            HttpResponse(StatusCodes.InternalServerError, entity = "Bad things happen, for example now")
          case RatesError.System(underlying) =>
            HttpResponse(StatusCodes.InternalServerError, entity = s"Bad thing happened: ${underlying.getMessage()}")
        }
      )
    }

  implicit def zioSupportIOMarshaller[A, E](
    implicit ma: Marshaller[A, HttpResponse],
    me: Marshaller[E, HttpResponse]
  ): Marshaller[IO[E, A], HttpResponse] =
    Marshaller { implicit ec => a =>
      val r = a.foldZIO(
        e => ZIO.fromFuture(implicit ec => me(e)),
        a => ZIO.fromFuture(implicit ec => ma(a))
      )
      Unsafe.unsafe { implicit unsafe =>
        zio.Runtime.default.unsafe.runToFuture(r)
      }
    }
}
