import sbt._

object Dependencies {
  val circeVersion = "0.12.3"

  lazy val scalaTest = Seq("org.scalatest" %% "scalatest" % "3.0.8" % Test)

  lazy val circe = Seq(
    "io.circe" %% "circe-core" % circeVersion,
    "io.circe" %% "circe-generic" % circeVersion,
    "io.circe" %% "circe-parser" % circeVersion
  )

  lazy val `bouncy-castle` = Seq(
    "org.bouncycastle" % "bcprov-jdk15on" % "1.62",
    "org.bouncycastle" % "bcpkix-jdk15on" % "1.62"
  )

  lazy val jose = Seq(
    "org.bitbucket.b_c" % "jose4j" % "0.7.0" % Test,
    "com.nimbusds" % "nimbus-jose-jwt" % "9.0" % Test
  )

  lazy val scopt = Seq(
    "com.github.scopt" %% "scopt" % "4.0.0-RC2"
  )

  lazy val jsonLd = Seq(
    "com.apicatalog" % "titanium-json-ld" % "0.8.5",
    "org.glassfish" % "jakarta.json" % "1.1.6"
  )
}
