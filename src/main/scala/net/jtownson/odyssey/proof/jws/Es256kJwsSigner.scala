package net.jtownson.odyssey.proof.jws

import java.net.URI
import java.security.{PrivateKey, PublicKey}

import io.circe.Json
import io.circe.syntax.EncoderOps
import net.jtownson.odyssey.VerificationError.ParseError
import net.jtownson.odyssey.impl.CodecStuff.uriEncoder
import net.jtownson.odyssey.impl.Futures
import net.jtownson.odyssey.proof.JwsSigner
import net.jtownson.odyssey.proof.crypto.Ecdsa
import net.jtownson.odyssey.{Jws, PublicKeyResolver}
import org.bouncycastle.asn1.sec.SECNamedCurves

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class Es256kJwsSigner(publicKeyRef: URI, privateKey: PrivateKey)(implicit ec: ExecutionContext) extends JwsSigner {

  val signer = new Ecdsa(SECNamedCurves.getByName("secp256k1"))

  override def sign(data: Array[Byte]): Future[Array[Byte]] =
    Future.successful(signer.sign(data, privateKey))

  override def getAlgorithmHeaders: Map[String, Json] =
    Map("alg" -> "ES256K".asJson, "kid" -> publicKeyRef.asJson)

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
    signer.verify(data, signature, publicKey)
  }

  private def safeParseUri(uriStr: String): Either[ParseError, URI] = {
    Try(URI.create(uriStr)).toEither.left.map(t => ParseError(t.getMessage))
  }

  def verifyAlgorithmHeaders(headers: Map[String, Json]): Boolean = {
    headers.get("alg").flatMap(_.asString).forall(_.equalsIgnoreCase("ES256K"))
  }

}
