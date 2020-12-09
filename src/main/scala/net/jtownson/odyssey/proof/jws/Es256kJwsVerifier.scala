package net.jtownson.odyssey.proof.jws

import java.net.URI
import java.security.PublicKey
import java.security.interfaces.ECPublicKey

import com.auth0.jwt.algorithms.DER
import io.circe.Json
import net.jtownson.odyssey.proof.JwsVerifier
import net.jtownson.odyssey.proof.JwsVerifier.VerificationResult
import net.jtownson.odyssey.proof.crypto.Ecdsa
import net.jtownson.odyssey.{Jws, PublicKeyResolver}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class Es256kJwsVerifier(publicKeyResolver: PublicKeyResolver)(implicit ec: ExecutionContext) extends JwsVerifier {

  override def verify(jws: Jws): Future[VerificationResult] = {
    verify(jws, jws.protectedHeaders, Jws.signingInput(jws), jws.signature)
  }

  private def verify(
      jws: Jws,
      headers: Map[String, Json],
      signerInput: Array[Byte],
      signature: Array[Byte]
  ): Future[VerificationResult] = {

    def invalidHeaders = VerificationResult(headersValid = false, signatureValid = false)

    val algoHeadersValid = verifyAlgorithmHeaders(headers)

    if (algoHeadersValid) {
      val maybeKidHeader = headers.get("kid")

      maybeKidHeader.fold(Future.successful(invalidHeaders)) { kidJson => // verify kid header present
        kidJson.asString.fold(Future.successful(invalidHeaders)) { kid => // verify kid is a JSON string
          Try(new URI(kid)).fold( // verify kid is a valid URL
            _ => Future.successful(invalidHeaders),
            kidUrl =>
              publicKeyResolver
                .resolvePublicKey(kidUrl)
                .map { publicKey => // verify the key resolves
                  doVerify(jws, signerInput, signature, publicKey) // finally check the signature
                }
          )
        }
      }
    } else {
      Future.successful(invalidHeaders)
    }
  }

  private def doVerify(
      jws: Jws,
      signerInput: Array[Byte],
      signature: Array[Byte],
      publicKey: PublicKey
  ): VerificationResult = {
    publicKey match {
      case ecPublicKey: ECPublicKey =>
        doVerify(signerInput, signature, ecPublicKey)
      case _ =>
        VerificationResult(headersValid = true, signatureValid = false)
    }
  }

  private def doVerify(signerInput: Array[Byte], signature: Array[Byte], publicKey: ECPublicKey): VerificationResult = {
    if (Ecdsa.verify(signerInput, DER.JOSEToDER(signature), publicKey)) {
      VerificationResult(headersValid = true, signatureValid = true)
    } else
      VerificationResult(headersValid = true, signatureValid = false)
  }

  def verifyAlgorithmHeaders(headers: Map[String, Json]): Boolean = {

    /** TODO
      * https://tools.ietf.org/html/draft-ietf-cose-webauthn-algorithms-03#section-3.1
      * o  The "crv" field MUST be present and it MUST represent the
      * "secp256k1" elliptic curve.
      *
      * o  If the "alg" field is present, it MUST represent the "ES256K"
      * algorithm.
      *
      * o  If the "key_ops" field is present, it MUST include "sign" when
      * creating an ECDSA signature.
      *
      * o  If the "key_ops" field is present, it MUST include "verify" when
      * verifying an ECDSA signature.
      *
      * o  If the JWK _use_ field is present, its value MUST be "sig".*/

    headers.get("alg").flatMap(_.asString).forall(_.equalsIgnoreCase("ES256K"))
  }
}
