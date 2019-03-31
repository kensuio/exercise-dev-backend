package forex.interfaces.api.rates

import forex.domain._
import io.circe._
import io.circe.generic.semiauto._

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
    implicit val encoder: Encoder[GetApiResponse] = deriveEncoder[GetApiResponse]
  }

  final case class OneForgeResponse(
      symbol: String,
      price: Double,
      bid: Double,
      ask: Double,
      timestamp: Long
  )

  object OneForgeResponse {
    implicit val decoder: Decoder[OneForgeResponse] = deriveDecoder[OneForgeResponse]
  }

}
