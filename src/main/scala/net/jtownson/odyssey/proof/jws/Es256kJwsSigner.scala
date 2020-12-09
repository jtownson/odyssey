package net.jtownson.odyssey.proof.jws

import java.net.URI
import java.security.PublicKey
import java.security.interfaces.{ECPrivateKey, ECPublicKey}

import com.auth0.jwt.algorithms.DER.DERToJOSE
import io.circe.Json
import io.circe.syntax.EncoderOps
import net.jtownson.odyssey.VerificationError.ParseError
import net.jtownson.odyssey.impl.CodecStuff.uriEncoder
import net.jtownson.odyssey.impl.Futures
import net.jtownson.odyssey.proof.JwsSigner
import net.jtownson.odyssey.proof.crypto.Ecdsa
import net.jtownson.odyssey.{Jws, PublicKeyResolver}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class Es256kJwsSigner(publicKeyRef: URI, privateKey: ECPrivateKey)(implicit ec: ExecutionContext) extends JwsSigner {

  override def sign(data: Array[Byte]): Future[Array[Byte]] = {
    // See https://tools.ietf.org/html/draft-ietf-cose-webauthn-algorithms-03#section-3.2
    // for sig formaula
    val joseSig = DERToJOSE(Ecdsa.sign(data, privateKey))

    Future.successful(joseSig)
  }

  override def getAlgorithmHeaders: Map[String, Json] =
    Map("kty" -> "EC".asJson, "alg" -> "ES256K".asJson, "crv" -> "secp256k1".asJson, "kid" -> publicKeyRef.asJson)

  def verify(jws: Jws, keyResolver: PublicKeyResolver): Future[Boolean] = {
    val kid: Either[ParseError, URI] = for {
      _ <- Either.cond(
        verifyAlgorithmHeaders(jws.protectedHeaders),
        true,
        ParseError("Incorrect headers for ES256 verification")
      )
      keyIdStr <- jws.protectedHeaders.get("kid").flatMap(_.asString).toRight(ParseError("No kid present in headers."))
      keyId <- safeParseUri(keyIdStr)
    } yield {
      keyId
    }

    for {
      keyRef <- Futures.toFuture(kid)
      publicKey <- keyResolver.resolvePublicKey(keyRef)
    } yield {
      verify(Jws.signingInput(jws), jws.signature, publicKey)
    }
  }

  def verify(data: Array[Byte], signature: Array[Byte], publicKey: PublicKey): Boolean = {
    publicKey match {
      case ecPublicKey: ECPublicKey => Ecdsa.verify(data, signature, ecPublicKey)
      case _ => false
    }
  }

  private def safeParseUri(uriStr: String): Either[ParseError, URI] = {
    Try(URI.create(uriStr)).toEither.left.map(t => ParseError(t.getMessage))
  }

  def verifyAlgorithmHeaders(headers: Map[String, Json]): Boolean = {
    headers.get("alg").flatMap(_.asString).forall(_.equalsIgnoreCase("ES256K"))
  }

}
