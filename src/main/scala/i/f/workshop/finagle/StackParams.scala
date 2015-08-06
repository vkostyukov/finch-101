package i.f.workshop.finagle

import com.twitter.finagle.param
import com.twitter.finagle.httpx.{Response, Request}
import com.twitter.finagle.service.TimeoutFilter
import com.twitter.finagle.transport.Transport
import com.twitter.finagle.{ListeningServer, Httpx, Service}
import com.twitter.util._
import com.twitter.conversions.time._

object StackParams extends App {
  implicit val timer: Timer = new JavaTimer()
  val service: Service[Request, Response] = Service.mk { req: Request =>
    Future.sleep(5.seconds).map(_ => Response())
  }

  val monitor: Monitor = new Monitor {
    override def handle(exc: Throwable): Boolean = {
      println(exc)

      true
    }
  }

  val server: ListeningServer = Httpx.server
    .configured(TimeoutFilter.Param(3.second))
    .configured(Transport.Verbose(true))
    .configured(param.Monitor(monitor))
    .serve(":8081", service)

  Await.ready(server)
}
