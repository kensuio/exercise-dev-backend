package forex.domain

import zio.json.JsonEncoder
import scala.collection.immutable.SortedSet

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

  def fromStringOption(s: String): Option[Currency] = s match {
    case "AUD" | "aud" => Some(AUD)
    case "CAD" | "cad" => Some(CAD)
    case "CHF" | "chf" => Some(CHF)
    case "EUR" | "eur" => Some(EUR)
    case "GBP" | "gbp" => Some(GBP)
    case "NZD" | "nzd" => Some(NZD)
    case "JPY" | "jpy" => Some(JPY)
    case "SGD" | "sgd" => Some(SGD)
    case "USD" | "usd" => Some(USD)
    case _ => None
  }

  /** While the set of known currencies is (very) small, we can just get the n-choose-2 pairs every time.
   *  (2*(n-choose-2) if we do not query for currency-pairs solely in a canonical ordering and invert the result if necessary)
   *
   *  We might use reflection/macros here - but this is a complication that was already deemed not with the hassle
   *  in the code above.
   */
  def allKnown: Set[Currency] = Set(AUD, CAD, CHF, EUR, GBP, NZD, JPY, SGD, USD)
  def allKnownPairs: Set[(Currency, Currency)] = allKnown.subsets(2).map(_.toSeq.sorted).map { case Seq(a, b) => (a, b) }.toSet

  implicit val encoder: JsonEncoder[Currency] = JsonEncoder[String].contramap[Currency](toString)
  /** For configurable optimization of canonicalizing currency-pairs before querying and caching (halves cache misses) */
  implicit val ordering: Ordering[Currency] = Ordering.by(toString)
}

