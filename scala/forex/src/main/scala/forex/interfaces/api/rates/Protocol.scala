package forex.interfaces.api.rates

import forex.domain._
import zio.json.{DeriveJsonEncoder, JsonEncoder}

object Protocol {

  final case class GetApiRequest(
    from: Currency,
    to: Currency
  )

  final case class GetApiResponse(
    from: Currency,
    to: Currency,
    price: Price,
    timestamp: Timestamp
  )

  object GetApiResponse {
    implicit val encoder: JsonEncoder[GetApiResponse] = DeriveJsonEncoder.gen[GetApiResponse]
  }

}
