name := "finch-101"
version := "0.0.0"
scalaVersion := "2.11.7"

resolvers += Resolver.sonatypeRepo("snapshots")

resolvers += "TM" at "http://maven.twttr.com"

libraryDependencies ++= Seq(
  "com.github.finagle" %% "finch-core" % "0.9.2" changing(),
  "com.github.finagle" %% "finch-circe" % "0.9.2" changing(),
  "io.circe" %% "circe-generic" % "0.3.0" changing(),
  "com.twitter" %% "twitter-server" % "1.15.0",
  "com.twitter" %% "finagle-stats" % "6.30.0"
)

initialCommands in console :=
  """
    |import io.finch._
    |import io.finch.circe._
    |import io.circe.generic.auto._
    |import com.twitter.finagle.Http
    |import com.twitter.finagle.Service
    |import com.twitter.finagle.http.{Request, Response}
    |import com.twitter.util.{Future, Await}
  """.stripMargin
