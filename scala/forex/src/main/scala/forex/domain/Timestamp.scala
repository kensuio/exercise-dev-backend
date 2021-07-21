package forex.domain

import java.time.OffsetDateTime

import zio.json.JsonEncoder

final case class Timestamp(value: OffsetDateTime) extends AnyVal

object Timestamp {

  def now: Timestamp = Timestamp(OffsetDateTime.now)

  implicit val encoder: JsonEncoder[Timestamp] = JsonEncoder[OffsetDateTime].contramap(_.value)
}
