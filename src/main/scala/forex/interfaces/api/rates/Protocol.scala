package forex.interfaces.api.rates

import java.time.OffsetDateTime

import forex.domain._
import zio.json.{DeriveJsonEncoder, JsonEncoder}

/** Defines http endpoint protocol objects for request/response calls */
object Protocol {

  final case class GetApiRequest(
    from: Currency,
    to: Currency
  )

  final case class GetApiResponse(
    from: Currency,
    to: Currency,
    price: Price,
    timestamp: OffsetDateTime
  )

  object GetApiResponse {
    implicit val encoder: JsonEncoder[GetApiResponse] = DeriveJsonEncoder.gen[GetApiResponse]
  }

}
