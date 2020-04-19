package net.jtownson.odyssey

import java.net.URL
import java.security.{PrivateKey, PublicKey}
import java.time.{LocalDateTime, ZoneOffset}

import io.circe.Printer
import io.circe.parser.decode
import net.jtownson.odyssey.VerificationError.{InvalidSignature, ParseError}
import org.jose4j.jwa.AlgorithmConstraints
import org.jose4j.jwa.AlgorithmConstraints.ConstraintType
import org.jose4j.jws.{AlgorithmIdentifiers, JsonWebSignature}

// Encode and decode verifiable credentials as application/vc+json+jwt
object JwsCodec {

  def encodeJws(privateKey: PrivateKey, publicKeyRef: URL, signatureAlgo: String, vc: VC): String = {
    val jws: JsonWebSignature = new JsonWebSignature()
    jws.setAlgorithmHeaderValue(signatureAlgo)
    jws.setKey(privateKey)
    jws.setHeader("vc", VcJsonCodec.vcJsonEncoder(vc).printWith(Printer.spaces2))
    jws.setHeader("kid", publicKeyRef.toString)
    jws.setHeader("cty", "application/vc+json+jwt")
    jws.setHeader("iss", vc.issuer.toString)
    vc.issuanceDate.foreach(iss => jws.setHeader("nbf", iss.toEpochSecond(ZoneOffset.UTC)))
    vc.expirationDate.foreach(exp => jws.setHeader("exp", exp.toEpochSecond(ZoneOffset.UTC)))
    jws.getCompactSerialization
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
      val vc: String = jws.getHeader("vc")

      def optionalHeader(jws: JsonWebSignature, name: String): Option[String] = {
        Option(jws.getObjectHeader(name)).map(_.toString)
      }

      def optionalTimestampHeader(jws: JsonWebSignature, name: String): Option[LocalDateTime] = {
        optionalHeader(jws, name).map(v => LocalDateTime.ofEpochSecond(v.toLong, 0, ZoneOffset.UTC))
      }

      // TODO check what spec says about duplicated fields in jwt headers and the embedded vc
      val issuanceDate = optionalTimestampHeader(jws, "nbf")
      val expirationDate = optionalTimestampHeader(jws, "exp")

      decode[VC](vc)(VcJsonCodec.vcJsonDecoder).left.map(_ => ParseError)

    } else {
      Left(InvalidSignature)
    }
  }

}
