package net.jtownson.odyssey

import java.net.URL
import java.security.{PrivateKey, PublicKey}
import java.time.ZoneOffset

import io.circe
import io.circe.syntax._
import io.circe.{Json, Printer, parser}
import net.jtownson.odyssey.VerificationError.{InvalidSignature, ParseError}
import org.jose4j.jwa.AlgorithmConstraints
import org.jose4j.jwa.AlgorithmConstraints.ConstraintType
import org.jose4j.jws.JsonWebSignature

import scala.concurrent.{ExecutionContext, Future}

// Encode and decode verifiable credentials as application/vc+json+jwt
object VCJwsCodec {
  import CodecStuff._
  def encodeJws(privateKey: PrivateKey, publicKeyRef: URL, signatureAlgo: String, vc: VC): String = {
    val jws: JsonWebSignature = new JsonWebSignature()
    jws.setKey(privateKey)
    jws.getHeaders.setFullHeaderAsJsonString(headers(publicKeyRef, signatureAlgo, vc))
    jws.getCompactSerialization
  }

  def decodeJws(algoWhitelist: Seq[String], publicKeyResolver: PublicKeyResolver, jwsSer: String)(implicit
      ec: ExecutionContext
  ): Future[VC] = {
    val jws = new JsonWebSignature()
    jws.setCompactSerialization(jwsSer)
    jws.setAlgorithmConstraints(new AlgorithmConstraints(ConstraintType.WHITELIST, algoWhitelist: _*))
    val publicKeyRef = new URL(jws.getHeader("kid"))

    for {
      publicKey <- publicKeyResolver.resolvePublicKey(publicKeyRef)
      _ <- verifySignature(jws, publicKey)
      vc <- toFuture(parseVc(jws))
    } yield {
      vc
    }
  }

  private def headers(publicKeyRef: URL, signatureAlgo: String, vc: VC): String = {
    import VCJsonCodec._
    Json
      .obj(
        "cty" -> "application/vc+json".asJson,
        "jti" -> vc.id.map(id => id.asJson).getOrElse(Json.Null),
        "kid" -> publicKeyRef.asJson,
        "alg" -> signatureAlgo.asJson,
        "iss" -> vc.issuer.asJson,
        "nbf" -> vc.issuanceDate.toEpochSecond(ZoneOffset.UTC).asJson,
        "exp" -> vc.expirationDate.map(exp => exp.toEpochSecond(ZoneOffset.UTC).asJson).getOrElse(Json.Null),
        "vc" -> vcJsonEncoder(vc)
      )
      .dropNullValues
      .printWith(Printer.spaces2)
  }

  private def verifySignature(jws: JsonWebSignature, publicKey: PublicKey): Future[Unit] = {
    jws.setKey(publicKey)
    if (jws.verifySignature())
      Future.unit
    else
      Future.failed(InvalidSignature())
  }

  private def toFuture(e: Either[circe.Error, VC]): Future[VC] = {
    e.left.map(circeError => ParseError(circeError.getMessage)).fold(Future.failed, Future.successful)
  }

  private def parseVc(jws: JsonWebSignature): Either[circe.Error, VC] = {
    for {
      headerJson <- parser.parse(jws.getHeaders.getFullHeaderAsJsonString)
      hc = headerJson.hcursor
      vcJson <- hc.downField("vc").as[Json]
      vc <- VCJsonCodec.vcJsonDecoder(vcJson.hcursor)
    } yield vc
  }
}
