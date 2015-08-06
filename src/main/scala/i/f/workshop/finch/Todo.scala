package i.f.workshop.finch

import java.util.UUID

import scala.collection.mutable
import com.twitter.finagle.{Httpx, ListeningServer}
import com.twitter.server.TwitterServer
import com.twitter.util.Await
import io.finch.request._
import io.finch.route._

object Todo extends TwitterServer {

  case class Todo(id: String, title: String, completed: Boolean, order: Int)

  object Todo {
    private[this] val db: mutable.Map[String, Todo] = mutable.Map.empty[String, Todo]

    def get(id: String): Option[Todo] = synchronized { db.get(id) }
    def list(): List[Todo] = synchronized { db.values.toList }
    def save(t: Todo): Unit = synchronized { db += (t.id -> t) }
  }

  val todo: RequestReader[Todo] =
    body.as[String => Todo].map(_(UUID.randomUUID().toString))

  val getTodos: Router[List[Todo]] = get("todos") {
    Todo.list()
  }

  val postTodo: Router[Todo] = post("todos" ? todo ) { t: Todo =>
    Todo.save(t); t
  }

  val patchTodo: Router[Todo] = patch("todos" / string) { id: String =>

  }

  val server: ListeningServer =
    Httpx.serve(":8081", (getTodos :+: postTodo :+: patchTodo).toService)

  onExit { server.close() }

  def main(): Unit = {
    Await.ready(adminHttpServer)
  }
}
