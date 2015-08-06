package i.f.workshop.finch

import com.twitter.finagle.{ListeningServer, Httpx}
import com.twitter.util.Await

import io.finch.request._
import io.finch.route._

object Greetings extends App {

  val hi: Router[String] =
    get("hi" / string) { name: String =>
      s"Hi, $name"
    }

  val title: RequestReader[String] = paramOption("title").withDefault("")
  val hello: Router[String] =
    get("hello" / string ? title) { (name: String, title: String) =>
      s"Hello, $title$name"
    }

  case class Who(name: String, title: String)
  val who: RequestReader[Who] = (param("name") :: title).as[Who]
  val salute: Router[String] =
    get("salute" ? who) { w: Who =>
      s"Salute, ${w.title}${w.name}"
    }

  val server: ListeningServer =
    Httpx.server.serve(":8081", (hi :+: hello :+: salute).toService)

  Await.ready(server)
}
