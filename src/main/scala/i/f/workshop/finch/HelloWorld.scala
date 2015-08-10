package i.f.workshop.finch

import com.twitter.finagle.Httpx
import com.twitter.util.Await

import io.finch.route._

object HelloWorld extends App {
  val hello: Router[String] = Router.value("Hello, World!")
  Await.ready(Httpx.server.serve(":8081", hello.toService))
}
