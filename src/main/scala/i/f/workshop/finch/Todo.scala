package i.f.workshop.finch

import java.util.UUID

import com.twitter.app.Flag
import com.twitter.finagle.param.Stats
import com.twitter.finagle.stats.Counter

import scala.collection.mutable
import com.twitter.finagle.{Httpx, ListeningServer, Service, SimpleFilter}
import com.twitter.finagle.httpx.{Request, Response}
import com.twitter.server.TwitterServer
import com.twitter.util.{Future, Await}

import io.finch.request._
import io.finch.response._
import io.finch.route._
import io.finch.circe._
import io.circe.generic.auto._

object Todo extends TwitterServer {

  val port: Flag[Int] = flag("port", 8081, "TCP port for HTTP server")

  case class Todo(id: String, title: String, completed: Boolean, order: Int)

  object Todo {
    private[this] val db: mutable.Map[String, Todo] = mutable.Map.empty[String, Todo]

    def get(id: String): Option[Todo] = synchronized { db.get(id) }
    def list(): List[Todo] = synchronized { db.values.toList }
    def save(t: Todo): Unit = synchronized { db += (t.id -> t) }
    def delete(id: String): Unit = synchronized { db -= id }
  }

  val todos: Counter = statsReceiver.counter("todos")

  val postedTodo: RequestReader[Todo] =
    body.as[String => Todo].map(_(UUID.randomUUID().toString))

  val getTodos: Router[List[Todo]] = get("todos") {
    Todo.list()
  }

  val postTodo: Router[Todo] = post("todos" ? postedTodo) { t: Todo =>
    todos.incr()
    Todo.save(t)

    t
  }

  case class TodoNotFound(id: String) extends Exception(s"Todo($id) not found.")
  val deleteTodo: Router[Todo] = delete("todos" / string) { id: String =>
    Todo.get(id) match {
      case Some(t) => Todo.delete(id); t
      case None => throw new TodoNotFound(id)
    }
  }

  val deleteTodos: Router[List[Todo]] = delete("todos") {
    val all: List[Todo] = Todo.list()
    all.foreach(t => Todo.delete(t.id))

    all
  }

  val patchedTodo: RequestReader[Todo => Todo] = body.as[Todo => Todo]

  val patchTodo: Router[Todo] =
    patch("todos" / string ? patchedTodo) { (id: String, pt: Todo => Todo) =>
      Todo.get(id) match {
        case Some(currentTodo) =>
          val newTodo: Todo = pt(currentTodo)
          Todo.delete(id)
          Todo.save(newTodo)

          newTodo
        case None => throw TodoNotFound(id)
      }
    }

  val handleExceptions: SimpleFilter[Request, Response] = new SimpleFilter[Request, Response] {
    def apply(req: Request, service: Service[Request, Response]): Future[Response] =
      service(req).handle {
        case e: RequestError => BadRequest(Map("message" -> e.message))
        case TodoNotFound(id) => NotFound(Map("id" -> id))
      }
  }

  val api: Service[Request, Response] = handleExceptions andThen (
    getTodos :+: postTodo :+: deleteTodo :+: deleteTodos :+: patchTodo
  ).toService

  def main(): Unit = {
    val server: ListeningServer = Httpx.server
      .configured(Stats(statsReceiver))
      .serve(s":${port()}", api)

    onExit { server.close() }

    Await.ready(adminHttpServer)
  }
}
