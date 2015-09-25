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

import io.finch._
import io.finch.request._
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

  val getTodos: Endpoint[List[Todo]] = get("todos") {
    Ok(Todo.list())
  }

  val postTodo: Endpoint[Todo] = post("todos" ? postedTodo) { t: Todo =>
    todos.incr()
    Todo.save(t)

    Created(t)
  }

  case class TodoNotFound(id: String) extends Exception(s"Todo($id) not found.")
  val deleteTodo: Endpoint[Todo] = delete("todos" / string) { id: String =>
    Todo.get(id) match {
      case Some(t) => Todo.delete(id); Ok(t)
      case None => throw new TodoNotFound(id)
    }
  }

  val deleteTodos: Endpoint[List[Todo]] = delete("todos") {
    val all: List[Todo] = Todo.list()
    all.foreach(t => Todo.delete(t.id))

    Ok(all)
  }

  val patchedTodo: RequestReader[Todo => Todo] = body.as[Todo => Todo]

  val patchTodo: Endpoint[Todo] =
    patch("todos" / string ? patchedTodo) { (id: String, pt: Todo => Todo) =>
      Todo.get(id) match {
        case Some(currentTodo) =>
          val newTodo: Todo = pt(currentTodo)
          Todo.delete(id)
          Todo.save(newTodo)

          Ok(newTodo)
        case None => throw TodoNotFound(id)
      }
    }

  val handleExceptions: SimpleFilter[Request, Response] = new SimpleFilter[Request, Response] {
    def apply(req: Request, service: Service[Request, Response]): Future[Response] =
      service(req).handle {
        case TodoNotFound(id) => io.finch.response.NotFound(Map("id" -> id))
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
