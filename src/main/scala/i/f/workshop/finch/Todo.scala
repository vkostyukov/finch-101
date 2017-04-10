package i.f.workshop.finch

import java.util.UUID

import com.twitter.app.Flag
import com.twitter.finagle.param.Stats
import com.twitter.finagle.stats.Counter

import scala.collection.mutable
import com.twitter.finagle.{Http, ListeningServer, Service, SimpleFilter}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.server.TwitterServer
import com.twitter.util.{Future, Await}

import io.finch._
import io.finch.circe._
import io.circe.generic.auto._

import io.circe._

object Todo extends TwitterServer {

  val port: Flag[Int] = flag("port", 8081, "TCP port for HTTP server")

  case class Todo(id: UUID, title: String, completed: Boolean, order: Int)
  case class TodoNotFound(id: UUID) extends Exception {
    override def getMessage: String = s"Todo(${id.toString}) not found."
  }

  implicit val encodeException: Encoder[Exception] = Encoder.instance(e =>
    Json.obj(
      "message" -> Json.string(e.getMessage)
    )
  )

  object Todo {
    private[this] val db: mutable.Map[UUID, Todo] = mutable.Map.empty[UUID, Todo]

    def get(id: UUID): Option[Todo] = synchronized { db.get(id) }
    def list(): List[Todo] = synchronized { db.values.toList }
    def save(t: Todo): Unit = synchronized { db += (t.id -> t) }
    def delete(id: UUID): Unit = synchronized { db -= id }
  }

  val todos: Counter = statsReceiver.counter("todos")

  val postedTodo: Endpoint[Todo] =
    body.as[UUID => Todo].map(_(UUID.randomUUID()))

  val getTodos: Endpoint[List[Todo]] = get("todos") {
    Ok(Todo.list())
  }

  val postTodo: Endpoint[Todo] = post("todos" ? postedTodo) { t: Todo =>
    todos.incr()
    Todo.save(t)

    Created(t)
  }

  val deleteTodo: Endpoint[Todo] = delete("todos" / uuid) { id: UUID =>
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

  val patchedTodo: Endpoint[Todo => Todo] = body.as[Todo => Todo]

  val patchTodo: Endpoint[Todo] =
    patch("todos" / uuid ? patchedTodo) { (id: UUID, pt: Todo => Todo) =>
      Todo.get(id) match {
        case Some(currentTodo) =>
          val newTodo: Todo = pt(currentTodo)
          Todo.delete(id)
          Todo.save(newTodo)

          Ok(newTodo)
        case None => throw TodoNotFound(id)
      }
    }

  val api: Service[Request, Response] = (
    getTodos :+: postTodo :+: deleteTodo :+: deleteTodos :+: patchTodo
  ).handle({
    case e: TodoNotFound => NotFound(e)
  }).toService

  def main(): Unit = {
    val server: ListeningServer = Http.server
      .configured(Stats(statsReceiver))
      .serve(s":${port()}", api)

    onExit { server.close() }

    Await.ready(adminHttpServer)
  }
}
