import sbt._

object Dependencies {
  val circeVersion = "0.12.3"

  lazy val scalaTest = Seq("org.scalatest" %% "scalatest" % "3.0.8" % Test)
  lazy val `circe-core` = Seq("io.circe" %% "circe-core" % circeVersion)
  lazy val `circe-generic` = Seq("io.circe" %% "circe-generic" % circeVersion)
  lazy val `circe-parser` = Seq("io.circe" %% "circe-parser" % circeVersion)
}
