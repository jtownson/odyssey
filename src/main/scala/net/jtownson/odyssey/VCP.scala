package net.jtownson.odyssey
import java.io.{File, FileOutputStream, FileWriter, PrintWriter}

import io.circe.{Json, Printer}
import scopt.OParser

import scala.io.Source
import scala.util.Using

/**
  * Embryonic app to enable running the verifiable credentials test suite.
  */
object VCP extends App {

  val builder = OParser.builder[Config]
  import builder._
  val parser = OParser.sequence(
    arg[File]("<file>")
      .action((file, c) => c.copy(file = Some(file)))
      .text("Input VC")
  )

  OParser.parse(parser, args, Config()).foreach { config: Config =>
    config.file.foreach { file =>
      Using(Source.fromFile(file, "UTF-8"))(_.mkString).foreach { jsonLd =>
        VC.fromJsonLd(jsonLd) match {
          case Left(err) =>
            System.err.println(s"Got an error: ${err.getMessage}")
          case Right(_) =>
            print(jsonLd)
        }
      }
    }
  }

  sealed trait Command
  case object HelpCommand extends Command

  case class Config(
      file: Option[File] = None
  )
}
