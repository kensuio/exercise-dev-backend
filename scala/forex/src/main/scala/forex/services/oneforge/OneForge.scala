package forex.services.oneforge

import forex.domain._
import zio._

trait OneForge {
  def get(pair: Rate.Pair): IO[OneForgeError, Rate]
}

final case class DummyOneForge() extends OneForge {

  override def get(pair: Rate.Pair): IO[OneForgeError, Rate] =
    UIO(Rate(pair, Price(2), Timestamp.now))
}

object OneForge {

  val dummy: ULayer[Has[OneForge]] =
    DummyOneForge.toLayer
}
