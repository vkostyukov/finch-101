package i.f.workshop.finch

import com.twitter.finagle.Http
import com.twitter.util.Await

import io.finch._
import io.finch.circe._
import io.circe.generic.auto._

object HelloWorld extends App {
  val hello: Endpoint[String] = Endpoint(Ok("Hello, World!"))
  Await.ready(Http.server.serve(":8081", hello.toService))
}
