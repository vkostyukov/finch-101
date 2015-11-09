package i.f.workshop.finagle

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Http, Service}
import com.twitter.util.Await

object Proxy extends App {
  val giphy: Service[Request, Response] = Http.client.newService("giphy.com:80")
  Await.ready(Http.server.serve(":8081", giphy))
}
