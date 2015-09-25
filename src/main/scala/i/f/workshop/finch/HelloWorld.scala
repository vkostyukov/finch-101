package i.f.workshop.finch

import com.twitter.finagle.Httpx
import com.twitter.util.Await

import io.finch._

object HelloWorld extends App {
  val hello: Endpoint[String] = Endpoint(Ok("Hello, World!"))
  Await.ready(Httpx.server.serve(":8081", hello.toService))
}
