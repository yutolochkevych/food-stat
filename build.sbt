organization in ThisBuild := "com.food.stats"

name := "food-stats"

version := "1.0"

scalaVersion  := "2.11.8"

lazy val http4sVersion = "0.14.11"

resolvers += "spray repo" at "http://repo.spray.io"

val sprayVersion = "1.3.3"
val akkaVersion = "2.4.8"

libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "io.spray" %% "spray-caching" % sprayVersion,
    "io.spray" %% "spray-can" % sprayVersion,
    "io.spray" %% "spray-routing" % sprayVersion,
    "io.spray" %% "spray-http" % sprayVersion,
    "io.spray" %% "spray-client" % sprayVersion,
    "io.spray" %%  "spray-json" % "1.3.2",
    "org.json4s" %% "json4s-native" % "3.3.0",
    "org.json4s" %% "json4s-jackson" % "3.3.0",
    "org.scalatest" %% "scalatest" % "2.2.5",
    "io.spray" %% "spray-testkit" % sprayVersion,
    "org.apache.spark" % "spark-core_2.11" % "2.0.0",
    "org.http4s" %% "http4s-json4s-native" % http4sVersion,
    "org.http4s" %% "http4s-json4s-jackson" % http4sVersion,
    "org.http4s" %% "http4s-dsl" % http4sVersion,
    "org.http4s" %% "http4s-blaze-server" % http4sVersion,
    "org.http4s" %% "http4s-blaze-client" % http4sVersion
)

