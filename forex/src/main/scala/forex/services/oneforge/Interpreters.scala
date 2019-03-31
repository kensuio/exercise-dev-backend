package forex.services.oneforge

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import forex.domain._
import forex.interfaces.api.rates.Protocol.OneForgeResponse
import forex.interfaces.api.utils.ApiMarshallers
import monix.eval.Task
import org.atnos.eff._
import org.atnos.eff.addon.monix.task._

import scala.concurrent.Future

object Interpreters {

  def dummy[R](
      implicit
      m1: _task[R]
  ): Algebra[Eff[R, ?]] = new Dummy[R]

  def live[R](
      implicit
      m1: _task[R]
  ): Algebra[Eff[R, ?]] = new Live[R]
}

final class Dummy[R] private[oneforge] (
    implicit
    m1: _task[R]
) extends Algebra[Eff[R, ?]] {

  override def get(
      pair: Rate.Pair
  ): Eff[R, Error Either Rate] =
    for {
      result ← fromTask(Task.now(Rate(pair, Price(BigDecimal(100)), Timestamp.now)))
    } yield Right(result)
}

final class Live[R] private[oneforge] (
    implicit
    m1: _task[R]
) extends Algebra[Eff[R, ?]] {

  import ApiMarshallers._

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  override def get(
      pair: Rate.Pair
  ): Eff[R, Error Either Rate] = {

    def future: Future[Rate] = {
      Http().singleRequest(
        HttpRequest(
          uri = "https://forex.1forge.com/1.0.3/quotes?pairs=EURUSD&api_key=FSw6dDbDTfd3YyiLUzsOUR0AsfP8zfcU")
      ) flatMap  { response =>
        Unmarshal(response.entity).to[List[OneForgeResponse]].map(rs => Rate.fromOneForge(rs.head))
      }
    }

    for {
      result ← fromTask(Task.deferFuture(future))
    } yield Right(result)
  }
}