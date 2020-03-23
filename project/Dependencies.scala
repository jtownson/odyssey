import sbt._

object Dependencies {
  val circeVersion = "0.12.3"

  lazy val scalaTest = Seq("org.scalatest" %% "scalatest" % "3.0.8" % Test)
  lazy val `circe-core` = Seq("io.circe" %% "circe-core" % circeVersion)
  lazy val `circe-generic` = Seq("io.circe" %% "circe-generic" % circeVersion)
  lazy val `circe-parser` = Seq("io.circe" %% "circe-parser" % circeVersion)
  lazy val `json-ld-java` = Seq("com.github.jsonld-java" % "jsonld-java" % "0.13.0")
  lazy val `ld-signatures-java` = Seq("info.weboftrust" % "ld-signatures-java" % "0.3-SNAPSHOT")
}
