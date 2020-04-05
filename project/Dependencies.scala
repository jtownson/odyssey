import sbt._

object Dependencies {
  val circeVersion = "0.12.3"

  lazy val scalaTest = Seq("org.scalatest" %% "scalatest" % "3.0.8" % Test)

  lazy val circe = Seq(
    "io.circe" %% "circe-core" % circeVersion,
    "io.circe" %% "circe-generic" % circeVersion,
    "io.circe" %% "circe-parser" % circeVersion
  )

  lazy val `json-ld-java` = Seq("com.github.jsonld-java" % "jsonld-java" % "0.13.0")

  lazy val `ld-signatures-java` = Seq("info.weboftrust" % "ld-signatures-java" % "0.3-SNAPSHOT")

  lazy val `bouncy-castle` = Seq(
    "org.bouncycastle" % "bcprov-jdk15on" % "1.62",
    "org.bouncycastle" % "bcpkix-jdk15on" % "1.62"
  )

  lazy val jena = Seq(
    "org.apache.jena" % "apache-jena-libs" % "3.14.0"
  )

  lazy val jose = Seq(
    "org.bitbucket.b_c" % "jose4j" % "0.7.0"
  )

  lazy val cats = Seq(
    "org.typelevel" %% "cats-core" % "2.0.0"
  )
}
