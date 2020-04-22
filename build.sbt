import Dependencies._

lazy val commonSettings = Seq(
  scalacOptions := Seq(
    /*"-Xlog-implicits",*/ "-Ypartial-unification",
    "-unchecked",
    "-deprecation",
    "-feature",
    "-language:implicitConversions"
  )
)

ThisBuild / scalaVersion := "2.13.1"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "net.jtownson"
ThisBuild / organizationName := "odyssey"
ThisBuild / scalacOptions := Seq(
  /*"-Xlog-implicits",*/ "-Xfatal-warnings",
  "-unchecked",
  "-deprecation",
  "-feature",
  "-language:implicitConversions"
)

lazy val root = (project in file("."))
  .settings(
    name := "odyssey",
    mainClass in (Compile, packageBin) := Some("net.jtownson.odyssey.VCP"),
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", "versions", _*) => MergeStrategy.first
      case PathList("module-info.class", _*) => MergeStrategy.first
      case PathList("org", "slf4j", _*) => MergeStrategy.first
      case x => (assemblyMergeStrategy in assembly).value(x)
    },
    libraryDependencies ++=
      circe ++
        `bouncy-castle` ++
        jose ++
        scopt ++
        `slf4j-nop` ++
        scalaTest
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
