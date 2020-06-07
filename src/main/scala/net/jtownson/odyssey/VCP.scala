package net.jtownson.odyssey
import java.io.File

import net.jtownson.odyssey.impl.Using
import scopt.OParser

import scala.io.Source

/**
  * App to enable running the verifiable credentials test suite.
  * The principle of the app is to parse a credential or presentation
  * and echo those that it considers valid, or print an error.
  */
object VCP extends App {

  val builder = OParser.builder[Config]
  import builder._
  val parser = OParser.sequence(
    opt[String]('t', "type")
      .action((tpe, c) => c.copy(tpe = tpe))
      .validate(validateType),
    arg[File]("<file>")
      .action((file, c) => c.copy(file = Some(file)))
      .text("Input VC")
  )

  private def validateType(v: String): Either[String, Unit] = {
    if (v == "VerifiableCredential" || v == "VerifiablePresentation")
      success
    else
      failure("Invalid type. Must be either VerifiableCredential or VerifiablePresentation.")
  }

  OParser.parse(parser, args, Config()).foreach { config: Config =>
    config.file.foreach { file =>
      Using(Source.fromFile(file, "UTF-8"))(_.mkString).foreach { jsonLd =>
        if (config.tpe == "VerifiableCredential") {
          VC.fromJsonLd(jsonLd) match {
            case Left(err) =>
              System.err.println(s"Error processing verifiable credential: ${err.getMessage}")
            case Right(_) =>
              print(jsonLd)
          }
        } else {
          VP.fromJsonLd(jsonLd) match {
            case Left(err) =>
              System.err.println(s"Error processing verifiable presentation: ${err.getMessage}.")
            case Right(vp) =>
              println(jsonLd)
          }
        }
      }
    }
  }

  case class Config(
      file: Option[File] = None,
      tpe: String = "VerifiableCredential"
  )
}
