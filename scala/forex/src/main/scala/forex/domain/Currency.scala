package forex.domain

import zio.json.JsonEncoder

sealed trait Currency

/** Provides explicit definitions alogn with json representation */
object Currency {
  final case object AUD extends Currency
  final case object CAD extends Currency
  final case object CHF extends Currency
  final case object EUR extends Currency
  final case object GBP extends Currency
  final case object NZD extends Currency
  final case object JPY extends Currency
  final case object SGD extends Currency
  final case object USD extends Currency

  def toString(c: Currency) = c match {
    case AUD => "AUD"
    case CAD => "CAD"
    case CHF => "CHF"
    case EUR => "EUR"
    case GBP => "GBP"
    case NZD => "NZD"
    case JPY => "JPY"
    case SGD => "SGD"
    case USD => "USD"
  }

  def fromString(s: String): Currency = s match {
    case "AUD" | "aud" => AUD
    case "CAD" | "cad" => CAD
    case "CHF" | "chf" => CHF
    case "EUR" | "eur" => EUR
    case "GBP" | "gbp" => GBP
    case "NZD" | "nzd" => NZD
    case "JPY" | "jpy" => JPY
    case "SGD" | "sgd" => SGD
    case "USD" | "usd" => USD
  }

  implicit val encoder: JsonEncoder[Currency] = JsonEncoder[String].contramap[Currency](toString)
}
