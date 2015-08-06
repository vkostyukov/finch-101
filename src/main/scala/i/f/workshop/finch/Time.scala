package i.f.workshop.finch

import com.twitter.finagle.{ListeningServer, Httpx}
import com.twitter.util.Await

import io.finch.route._
import io.finch.request._

object Time extends App {

  case class Locale(language: String, country: String)

  def currentTime(l: java.util.Locale): String =
    java.util.Calendar.getInstance(l).getTime.toString

  val time: Router[String] =
    get("time" ? body.as[Locale]) { l: Locale =>
      currentTime(new java.util.Locale(l.language, l.country))
    }

  val server: ListeningServer = Httpx.server.serve(":8081", time.toService)
  Await.ready(server)
}
