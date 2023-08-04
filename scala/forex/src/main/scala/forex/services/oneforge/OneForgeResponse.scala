package forex.services.oneforge

import zio.json.{DeriveJsonDecoder, JsonDecoder}
import java.time.{Instant, OffsetDateTime, ZoneId}
import forex.domain.Rate
import forex.domain.Currency
import zio._
import forex.domain.Price

object OneForgeResponse {
  final case class PairQuote(
    private val s: String,
    private val p: BigDecimal,
    private val t: Long,
  ) {
    val pairDescriptor: String = s
    val rate: BigDecimal = p
    val timestamp: OffsetDateTime = OffsetDateTime.ofInstant(Instant.ofEpochMilli(t), ZoneId.systemDefault())
  }


  def pairQuoteToRate(pairQuote: PairQuote): Either[Throwable, Rate] = {
    val pairDescriptors = pairQuote.pairDescriptor.split("/")
    val leftRightDescOpt = pairDescriptors match {
      case Array(leftDesc, rightDesc) => Some((leftDesc, rightDesc))
      case _ => None
    }
    val currencyPairOpt = leftRightDescOpt.flatMap(pairDesc => {
      val leftCurrencyOpt = Currency.fromStringOption(pairDesc._1)
      val rightCurrencyOpt = Currency.fromStringOption(pairDesc._2)
      val pairOpt: Option[(Currency, Currency)] = leftCurrencyOpt zip rightCurrencyOpt
      pairOpt
    })

    currencyPairOpt match {
      case Some((leftCurrency, rightCurrency)) => {
        val pair = Rate.Pair(leftCurrency, rightCurrency)
        val price = Price(pairQuote.rate)
        val timestamp = pairQuote.timestamp
        val rate = Rate(pair, price, timestamp)
        Right(rate)
      }
      case None => Left(new Exception(s"Could not parse currency pair [${pairQuote.pairDescriptor}]"))
    }
  }

  def quoteToRates(quote: Seq[PairQuote]): Either[Throwable, Seq[Rate]] = {
    val rates = quote.map(pairQuoteToRate)
    val ratesEither = rates.partition(_.isRight)
    val ratesList = ratesEither match {
      case (Nil, _) => Left(new Exception("No rates found."))
      case (rates, Nil) => Right(rates.asInstanceOf[Seq[Right[Throwable, Rate]]].map(_.value))
      case (_, _) => Left(new Exception("Some rates could not be parsed."))
    }
    ratesList
  }

  implicit val pairQuoteDecoder: JsonDecoder[PairQuote] = DeriveJsonDecoder.gen[PairQuote]
  /* implicit val quoteDecoder: JsonDecoder[Seq[PairQuote]] = DeriveJsonDecoder.gen[Seq[PairQuote]] */
}
