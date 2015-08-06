package i.f.workshop.twitterserver

import com.twitter.server.TwitterServer
import com.twitter.util.Await

object HelloWorld extends TwitterServer {
  def main(): Unit = {
    Await.ready(adminHttpServer)
  }
}
