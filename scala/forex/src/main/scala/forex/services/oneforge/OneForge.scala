package forex.services.oneforge

import java.time.OffsetDateTime

import forex.domain._
import zio._

/** One-forge service specific client api */
trait OneForge {
  def get(pair: Rate.Pair): IO[OneForgeError, Rate]
}

final case class DummyOneForge() extends OneForge {

  override def get(pair: Rate.Pair): IO[OneForgeError, Rate] =
    ZIO.succeed(Rate(pair, Price(2), OffsetDateTime.now()))
}

object OneForge {

  // Dummy is not enough...
  val dummy: ULayer[OneForge] =
    ZLayer.succeed(DummyOneForge())
}
