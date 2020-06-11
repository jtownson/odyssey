import Dependencies._

ThisBuild / version := "0.1.5"
ThisBuild / organization := "net.jtownson"
ThisBuild / organizationName := "odyssey"
ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/jtownson/odyssey"),
    "scm:git@github.com:jtownson/odyssey.git"
  )
)
ThisBuild / developers := List(
  Developer(
    id = "jmt",
    name = "Jeremy Townson",
    email = "jeremy.townson@gmail.com",
    url = url("http://github.com/jtownson")
  )
)
ThisBuild / licenses := List("MIT" -> new URL("http://opensource.org/licenses/MIT"))
ThisBuild / homepage := Some(url("https://github.com/jtownson/odyssey"))
ThisBuild / pomIncludeRepository := { _ => false }
ThisBuild / publishTo := sonatypePublishToBundle.value
ThisBuild / publishMavenStyle := true

ThisBuild / scalaVersion := "2.12.10"
ThisBuild / scalacOptions := Seq(
  "-Xfatal-warnings",
  "-unchecked",
  "-deprecation",
  "-feature",
  "-language:implicitConversions"
)

lazy val scala212 = "2.12.10"
lazy val scala213 = "2.13.1"
ThisBuild / crossScalaVersions := List(scala212, scala213)

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
        scalaTest
  )
