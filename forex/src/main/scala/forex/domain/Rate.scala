package forex.domain

import forex.interfaces.api.rates.Protocol.OneForgeResponse
import io.circe._
import io.circe.generic.semiauto._

case class Rate(
    pair: Rate.Pair,
    price: Price,
    timestamp: Timestamp
)

object Rate {

  def fromOneForge(r: OneForgeResponse): Rate = {
    val (from, to) = r.symbol.splitAt(3)
    Rate(
      Rate.Pair(
        Currency.fromString(from),
        Currency.fromString(to)),
      Price(BigDecimal(r.price)),
      Timestamp(r.timestamp * 1000)
    )
  }

  final case class Pair(
      from: Currency,
      to: Currency
  )

  object Pair {
    implicit val encoder: Encoder[Pair] =
      deriveEncoder[Pair]
  }

  implicit val encoder: Encoder[Rate] =
    deriveEncoder[Rate]
}
