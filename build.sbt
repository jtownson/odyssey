import Dependencies._
import exec.runExec

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

ThisBuild / scalacOptions := Seq(
  "-Xfatal-warnings",
  "-unchecked",
  "-deprecation",
  "-feature",
  "-language:implicitConversions"
)

ThisBuild / javacOptions := Seq(
  "-source",
  "11",
  "-target",
  "11"
)

lazy val scala212 = "2.12.10"
lazy val scala213 = "2.13.1"
ThisBuild / crossScalaVersions := List(scala212, scala213)

val vctest = taskKey[Unit]("run w3c vc-test-suite")

vctest := {
  val npmWorkingDir = "w3c/vc-test-suite"
  val testCmd = "test"
  val installCmd = "install"
  val classpath = (fullClasspath in Test).value.files.mkString(":")
  val config = Seq((new File("w3c/config.json"), new File("w3c/vc-test-suite/config.json")))
  IO.copy(config, overwrite = true, preserveLastModified = true, preserveExecutable = false)

  runExec("npm", installCmd, npmWorkingDir, streams.value.log)
  runExec("npm", testCmd, npmWorkingDir, streams.value.log, ("CLASSPATH", classpath))
}

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
        jsonLd ++
        scalaTest
  )
