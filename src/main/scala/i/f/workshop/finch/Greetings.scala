package i.f.workshop.finch

import com.twitter.finagle.Httpx
import com.twitter.util.Await

import io.finch.request._
import io.finch.route._

object Greetings extends App {

  // GET /hi/:name
  val hi: Router[String] =
    get("hi" / string) { name: String =>
      s"Hi, $name!"
    }

  // GET /hello/:name?title=Mr.
  val title: RequestReader[String] = paramOption("title").withDefault("")
  val hello: Router[String] =
    get("hello" / string ? title) { (name: String, title: String) =>
      s"Hello, $title$name!"
    }

  // GET /salute?name=Bob&title=Mr.
  case class Who(name: String, title: String)
  val who: RequestReader[Who] = (param("name") :: title).as[Who]
  val salute: Router[String] =
    get("salute" ? who) { w: Who =>
      s"Salute, ${w.title}${w.name}!"
    }

  Await.ready(Httpx.serve(":8081", (hi :+: hello :+: salute).toService))

}
