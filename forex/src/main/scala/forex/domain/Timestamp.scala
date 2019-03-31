package forex.domain

import io.circe._
import io.circe.generic.extras.wrapped._
import io.circe.java8.time._
import java.time._

case class Timestamp(value: OffsetDateTime) extends AnyVal

object Timestamp {
  def now: Timestamp =
    Timestamp(OffsetDateTime.now)

  def apply(millis: Long): Timestamp = {
    val instant = Instant.ofEpochMilli(millis)
    val utc = ZoneOffset.UTC
    val localDateTime: LocalDateTime = instant.atZone(ZoneId.of(utc.getId)).toLocalDateTime
    Timestamp(OffsetDateTime.of(localDateTime, utc))
  }

  implicit val encoder: Encoder[Timestamp] =
    deriveUnwrappedEncoder[Timestamp]

  implicit val decoder: Decoder[Timestamp] =
    deriveUnwrappedDecoder[Timestamp]
}
