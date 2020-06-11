package net.jtownson.odyssey

import java.net.URI
import java.security.{PublicKey, Signature}

import io.circe.Json
import io.circe.syntax._
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

trait Verifier {
  def supportedAlgs: Seq[String]
  def verify(protectedHeaders: Map[String, Json], signerInput: Array[Byte], signature: Array[Byte])(implicit
      ec: ExecutionContext
  ): Future[Boolean]
}

object Verifier {

  // Example symmetric verifier
  class HmacSha256Verifier(secret: Array[Byte]) extends Verifier {

    private val key = new SecretKeySpec(secret, "HmacSHA256")

    def supportedAlgs: Seq[String] = Seq("HS256")

    override def verify(
        protectedHeaders: Map[String, Json],
        signerInput: Array[Byte],
        signature: Array[Byte]
    )(implicit ec: ExecutionContext): Future[Boolean] = {
      assert(protectedHeaders.get("alg").contains("HS256".asJson))

      val shahmac: Mac = Mac.getInstance("HmacSHA256")
      shahmac.init(key)
      val expectedSignature = shahmac.doFinal(signerInput)

      Future.successful(expectedSignature.toSeq == signature.toSeq)
    }
  }

  // example asymmetric verifier
  class Es256Verifier(publicKeyResolver: PublicKeyResolver) extends Verifier {
    import io.circe.syntax._
    override def supportedAlgs: Seq[String] = Seq("ES256")

    override def verify(protectedHeaders: Map[String, Json], signerInput: Array[Byte], signature: Array[Byte])(implicit
        ec: ExecutionContext
    ): Future[Boolean] = {
      assert(protectedHeaders.get("alg").contains("ES256".asJson)) // check for correct alg header

      val maybeKidHeader = protectedHeaders.get("kid")

      maybeKidHeader.fold(Future.successful(false)) { kidJson => // verify kid header present
        kidJson.asString.fold(Future.successful(false)) { kid => // verify kid is a JSON string
          Try(new URI(kid)).fold( // verify kid is a valid URL
            _ => Future.successful(false),
            kidUrl =>
              publicKeyResolver.resolvePublicKey(kidUrl).map { publicKey => // verify the key resolves
                doVerify(signerInput, signature, publicKey) // finally check the signature
              }
          )
        }
      }
    }

    private def doVerify(signerInput: Array[Byte], signature: Array[Byte], publicKey: PublicKey): Boolean = {
      val ecdsa: Signature = Signature.getInstance("SHA256withECDSA")
      ecdsa.initVerify(publicKey)
      ecdsa.update(signerInput)
      ecdsa.verify(signature)
    }
  }
}
