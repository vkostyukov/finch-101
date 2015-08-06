name := "finch-101"
version := "0.0.0"
scalaVersion := "2.11.7"
resolvers += Resolver.sonatypeRepo("snapshots")
libraryDependencies ++= Seq(
  "com.github.finagle" %% "finch-core" % "0.8.0-SNAPSHOT" changing(),
  "com.twitter" %% "twitter-server" % "1.12.0",
  "com.twitter" %% "finagle-stats" % "6.27.0"
)
