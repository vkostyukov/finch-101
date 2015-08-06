package i.f.workshop.finch

import com.twitter.finagle.{ListeningServer, Httpx}
import com.twitter.util.Await

import io.finch.route._

object HelloWorld extends App {

  val hello: Router[String] = Router.value("Hello, World!")

  val server: ListeningServer = Httpx.server.serve(":8081", hello.toService)

  Await.ready(server)
}
