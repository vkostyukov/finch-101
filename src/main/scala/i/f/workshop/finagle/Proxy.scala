package i.f.workshop.finagle

import com.twitter.finagle.httpx.{Request, Response}
import com.twitter.finagle.{ListeningServer, Httpx, Service}
import com.twitter.util.Await

object Proxy extends App {
  val giphy: Service[Request, Response] = Httpx.client.newService("giphy.com:80")
  val server: ListeningServer = Httpx.server.serve(":8081", giphy)

  Await.ready(server)
}
