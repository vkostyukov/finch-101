package i.f.workshop.finagle

import com.twitter.finagle.httpx.{Request, Response}
import com.twitter.finagle.{Httpx, Service}
import com.twitter.util.Await

object Proxy extends App {
  val giphy: Service[Request, Response] = Httpx.client.newService("giphy.com:80")
  Await.ready(Httpx.server.serve(":8081", giphy))
}
