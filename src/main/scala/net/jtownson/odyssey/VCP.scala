package net.jtownson.odyssey
import java.io.{File, FileOutputStream, FileWriter, PrintWriter}

import scopt.OParser

import scala.io.Source
import scala.util.Using

object VCP extends App {
//  import ch.qos.logback.classic.util.ContextInitializer
//  System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, "/logback.xml")

  val builder = OParser.builder[Config]
  import builder._
  val parser = OParser.sequence(
    arg[File]("<file>")
      .action((file, c) => c.copy(file = Some(file)))
      .text("Input VC")
  )
//  OParser.sequence(
//    programName("ConnectorClient"),
//    opt[String]('h', "host")
//      .valueName("<host>")
//      .action((x, c) => c.copy(host = x)),
//    opt[Int]('p', "port")
//      .valueName("<port>")
//      .action((x, c) => c.copy(port = x)),
//    opt[String]('u', "userId")
//      .action((userId, c) => c.copy(userId = Some(userId))),
//    cmd("register")
//      .action((x, c) => c.copy(command = Register())),

  OParser.parse(parser, args, Config()) match {
    case Some(config) =>
      config.file.foreach { file =>
        Using(Source.fromFile(file, "UTF-8"))(_.mkString).foreach { jsonLd =>
          val out = VC.fromJsonLd(jsonLd)
          out.rdfModel.write(System.out, "JSON-LD")
        }
      }
    case _ =>
    // arguments are bad, error message will have been displayed
  }

  sealed trait Command
  case object HelpCommand extends Command

  case class Config(
      file: Option[File] = None
  )
}
