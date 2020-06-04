package net.jtownson.odyssey

import io.circe.Json
import io.circe.syntax._
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import scala.concurrent.Future

trait Verifier {
  def supportedAlgs: Seq[String]
  def verify(protectedHeaders: Map[String, Json], signerInput: Array[Byte], signature: Array[Byte]): Future[Boolean]
}

object Verifier {

  class HMACSha256Verifier(secret: Array[Byte]) extends Verifier {

    private val key = new SecretKeySpec(secret, "HmacSHA256")

    def supportedAlgs: Seq[String] = Seq("HS256")

    override def verify(
        protectedHeaders: Map[String, Json],
        signerInput: Array[Byte],
        signature: Array[Byte]
    ): Future[Boolean] = {
      assert(protectedHeaders.get("alg").contains("HS256".asJson))

      val shahmac: Mac = Mac.getInstance("HmacSHA256")
      shahmac.init(key)
      val expectedSignature = shahmac.doFinal(signerInput)

      Future.successful(expectedSignature.deep == signature.deep)
    }
  }
}
