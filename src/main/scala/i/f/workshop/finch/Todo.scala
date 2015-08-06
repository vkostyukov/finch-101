package i.f.workshop.finch

import com.twitter.server.TwitterServer
import com.twitter.util.Await

object Todo extends TwitterServer {
  def main(): Unit = {
    Await.ready(adminHttpServer)
  }
}
