package forex.domain

import io.circe._
import io.circe.generic.extras.wrapped._

import java.time.OffsetDateTime

case class Timestamp(value: OffsetDateTime) extends AnyVal

object Timestamp {
  def now: Timestamp =
    Timestamp(OffsetDateTime.now)

  implicit val encoder: Encoder[Timestamp] =
    deriveUnwrappedEncoder[Timestamp]
}
