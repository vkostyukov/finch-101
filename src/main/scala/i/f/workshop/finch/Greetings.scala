package i.f.workshop.finch

import com.twitter.finagle.Http
import com.twitter.util.Await

import io.finch._
import io.finch.request._
import io.finch.circe._
import io.circe.generic.auto._

object Greetings extends App {

  // GET /hi/:name
  val hi: Endpoint[String] =
    get("hi" / string) { name: String =>
      Ok(s"Hi, $name!")
    }

  // GET /hello/:name?title=Mr.
  val title: RequestReader[String] = paramOption("title").withDefault("")
  val hello: Endpoint[String] =
    get("hello" / string ? title) { (name: String, title: String) =>
      Ok(s"Hello, $title$name!")
    }

  // GET /salute?name=Bob&title=Mr.
  case class Who(name: String, title: String)
  val who: RequestReader[Who] = (param("name") :: title).as[Who]
  val salute: Endpoint[String] =
    get("salute" ? who) { w: Who =>
      Ok(s"Salute, ${w.title}${w.name}!")
    }

  Await.ready(Http.serve(":8081", (hi :+: hello :+: salute).toService))
}
