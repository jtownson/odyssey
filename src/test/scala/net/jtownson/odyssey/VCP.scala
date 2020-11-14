package net.jtownson.odyssey

import java.io.File
import java.net.URI
import java.nio.charset.StandardCharsets.UTF_8
import java.util.Base64

import com.nimbusds.jose.jwk.ECKey
import io.circe.parser._
import net.jtownson.odyssey.Signer.Es256Signer
import net.jtownson.odyssey.impl.VPJsonCodec.vpJsonEncoder
import net.jtownson.odyssey.impl.{Using, VCJwsCodec}
import scopt.OParser

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.io.Source

/**
  * App to enable running the verifiable credentials test suite.
  * The principle of the app is to parse a credential or presentation
  * and echo those that it considers valid, or print an error.
  */
object VCP extends App {

  import io.circe._
  import net.jtownson.odyssey.impl.VCJsonCodec._

  val builder = OParser.builder[Config]
  import builder._

  val parser = OParser.sequence(
    opt[String]('t', "type")
      .action((tpe, c) => c.copy(tpe = tpe))
      .validate(validateType),
    opt[String]('j', "jwt")
      .action((jwt, config) => config.copy(jwt = Some(jwt))),
    opt[URI]('a', "jwt-aud")
      .action((aud, config) => config.copy(jwtAud = Some(aud))),
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
          import TestUtil.CirceFieldAccess
          VCDataModel.fromJsonLd(jsonLd) match {
            case Left(err) =>
              System.err.println(s"Error processing verifiable credential: ${err.getMessage}")
            case Right(vc) =>
              if (config.jwt.isDefined) {
                val jwtConfig = new String(Base64.getDecoder.decode(config.jwt.get), UTF_8)
                val hc =
                  parse(jwtConfig).getOrElse(throw new IllegalArgumentException("Invalid JWT Config JSON")).hcursor
                val keyJson = hc.jsonVal[Json]("es256kPrivateKeyJwk")
                val eCKey = ECKey.parse(Printer.noSpaces.print(keyJson))
                val signer = new Es256Signer(URI.create(eCKey.getKeyID), eCKey.toECPrivateKey)

                val jws = vc.toJws(signer)
                println(Await.result(jws.compactSerializion, Duration.Inf))
              } else {
                print(Printer.spaces2.print(vcJsonEncoder(vc)))
              }
          }
        } else {
          VPDataModel.fromJsonLd(jsonLd) match {
            case Left(err) =>
              System.err.println(s"Error processing verifiable presentation: ${err.getMessage}.")
            case Right(vp) =>
              println(Printer.spaces2.print(vpJsonEncoder(vp)))
          }
        }
      }
    }
  }

  case class Config(
      file: Option[File] = None,
      tpe: String = "VerifiableCredential",
      jwt: Option[String] = None,
      jwtAud: Option[URI] = None
  )

//  case class JwtConfig(
//    es256KPrivateKeyJws
//                      )
}
