import sbt._

object Dependencies {
  val circeVersion = "0.12.3"

  lazy val scalaTest = Seq("org.scalatest" %% "scalatest" % "3.0.8" % Test)

  lazy val circe = Seq(
    "io.circe" %% "circe-core" % circeVersion,
    "io.circe" %% "circe-parser" % circeVersion
  )

  lazy val `bouncy-castle` = Seq(
    "org.bouncycastle" % "bcprov-jdk15on" % "1.62",
    "org.bouncycastle" % "bcpkix-jdk15on" % "1.62"
  )

  lazy val `json-ld-java` = Seq(
    "com.github.jsonld-java" % "jsonld-java" % "0.13.0"
  )

  lazy val jena = Seq(
    "org.apache.jena" % "apache-jena-libs" % "3.14.0" exclude ("commons-logging", "commons-logging")
  )

  lazy val jose = Seq(
    "org.bitbucket.b_c" % "jose4j" % "0.7.0"
  )

  lazy val scopt = Seq(
    "com.github.scopt" %% "scopt" % "4.0.0-RC2"
  )

  lazy val `slf4j-nop` = Seq(
    "org.slf4j" % "slf4j-nop" % "1.7.26"
  )

}
