package net.jtownson.odyssey.proof.jws

import java.net.URI
import java.security.PublicKey

import io.circe.Json
import net.jtownson.odyssey.{Jws, PublicKeyResolver}
import net.jtownson.odyssey.proof.JwsVerifier
import net.jtownson.odyssey.proof.JwsVerifier.VerificationResult
import net.jtownson.odyssey.proof.crypto.Ed25519

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class Ed25519JwsVerifier(publicKeyResolver: PublicKeyResolver)(implicit ec: ExecutionContext) extends JwsVerifier {

  override def verify(jws: Jws): Future[VerificationResult] = {
    verify(jws.protectedHeaders, Jws.signingInput(jws), jws.signature)
  }

  private def verify(
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
                .map(publicKey => // verify the key resolves
                  doVerify(signerInput, signature, publicKey) // finally check the signature
                )
          )
        }
      }
    } else {
      Future.successful(invalidHeaders)
    }
  }

  private def doVerify(signerInput: Array[Byte], signature: Array[Byte], publicKey: PublicKey): VerificationResult = {
    VerificationResult(headersValid = true, signatureValid = Ed25519.verify(signerInput, signature, publicKey))
  }

  def verifyAlgorithmHeaders(headers: Map[String, Json]): Boolean = {
    headers.get("alg").flatMap(_.asString).forall(_.equalsIgnoreCase("EDDSA")) &&
    headers.get("crv").flatMap(_.asString).forall(_.equalsIgnoreCase("ED25519"))
  }
}
