package net.jtownson.odyssey

import java.net.URL
import java.security.{PrivateKey, PublicKey}
import java.time.{LocalDateTime, ZoneOffset}

import io.circe
import io.circe.{Json, Printer, parser}
import io.circe.parser.decode
import net.jtownson.odyssey.VerificationError.{InvalidSignature, ParseError}
import org.jose4j.jwa.AlgorithmConstraints
import org.jose4j.jwa.AlgorithmConstraints.ConstraintType
import org.jose4j.jws.{AlgorithmIdentifiers, JsonWebSignature}
import io.circe.syntax._

// Encode and decode verifiable credentials as application/vc+json+jwt
object JwsCodec {

  def encodeJws(privateKey: PrivateKey, publicKeyRef: URL, signatureAlgo: String, vc: VC): String = {
    val jws: JsonWebSignature = new JsonWebSignature()
//    jws.setAlgorithmHeaderValue(signatureAlgo)
    jws.setKey(privateKey)
    jws.getHeaders.setFullHeaderAsJsonString(header(publicKeyRef, signatureAlgo, vc))
//    jws.setKeyIdHeaderValue(publicKeyRef.toString)
//    jws.setContentTypeHeaderValue("application/vc+json")
//    jws.setHeader("iss", vc.issuer.toString)
//    vc.issuanceDate.foreach(iss => jws.setHeader("nbf", iss.toEpochSecond(ZoneOffset.UTC)))
//    vc.expirationDate.foreach(exp => jws.setHeader("exp", exp.toEpochSecond(ZoneOffset.UTC)))
    jws.getCompactSerialization
  }

  private def header(publicKeyRef: URL, signatureAlgo: String, vc: VC): String = {
    import JsonCodec._
    Json
      .obj(
        "cty" -> "application/vc+json".asJson,
        "kid" -> publicKeyRef.asJson,
        "alg" -> signatureAlgo.asJson,
        "iss" -> vc.issuer.asJson,
        "nbf" -> vc.issuanceDate.map(iss => iss.toEpochSecond(ZoneOffset.UTC).asJson).getOrElse(Json.Null),
        "exp" -> vc.expirationDate.map(exp => exp.toEpochSecond(ZoneOffset.UTC).asJson).getOrElse(Json.Null),
        "vc" -> vcJsonEncoder(vc)
      )
      .dropNullValues
      .printWith(Printer.spaces2)
  }

  // TODO parametrize public key resolvers and whitelisted signature algos.
  private def resolveVerificationKey(keyIdHeader: String): PublicKey = {
    KeyFoo.getPublicKeyFromRef(new URL(keyIdHeader))
  }

  def decodeJws(jwsSer: String): Either[VerificationError, VC] = {

    val jws = new JsonWebSignature()
    jws.setCompactSerialization(jwsSer)
    jws.setAlgorithmConstraints(
      new AlgorithmConstraints(ConstraintType.WHITELIST, AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256)
    )
    jws.setKey(resolveVerificationKey(jws.getHeader("kid")))

    if (jws.verifySignature()) {
      val parseResult: Either[circe.Error, VC] = for {
        headerJson <- parser.parse(jws.getHeaders.getFullHeaderAsJsonString)
        hc = headerJson.hcursor
        vcJson <- hc.downField("vc").as[Json]
        vc <- JsonCodec.vcJsonDecoder(vcJson.hcursor)
      } yield vc
      parseResult.left.map(circeError => ParseError)
    } else {
      Left(InvalidSignature)
    }
  }

}
