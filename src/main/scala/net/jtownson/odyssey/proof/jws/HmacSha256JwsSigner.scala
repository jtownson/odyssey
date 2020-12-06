package net.jtownson.odyssey.proof.jws

import io.circe.Json
import io.circe.syntax.EncoderOps
import net.jtownson.odyssey.Jws
import net.jtownson.odyssey.proof.JwsVerifier.VerificationResult
import net.jtownson.odyssey.proof.crypto.HmacSha256
import net.jtownson.odyssey.proof.{JwsSigner, JwsVerifier}

import scala.concurrent.{ExecutionContext, Future}

class HmacSha256JwsSigner(secret: Array[Byte])(implicit ec: ExecutionContext) extends JwsSigner with JwsVerifier {

  override def sign(data: Array[Byte]): Future[Array[Byte]] =
    Future.successful(HmacSha256.sign(data, secret))

  def verify(jws: Jws): Future[VerificationResult] = {
    val headersValid = verifyAlgorithmHeaders(jws.protectedHeaders)
    val result = if (headersValid) {
      val sigantureValid: Boolean = verifySignature(jws)
      VerificationResult(headersValid = true, sigantureValid)
    } else {
      VerificationResult(headersValid = false, signatureValid = false)
    }

    Future.successful(result)
  }

  private def verifySignature(jws: Jws): Boolean = {
    val expectedSignature = HmacSha256.sign(Jws.signingInput(jws.utf8ProtectedHeaders, jws.payload), secret)

    val sigantureValid = expectedSignature.toSeq == jws.signature.toSeq
    sigantureValid
  }

  def verifyAlgorithmHeaders(headers: Map[String, Json]): Boolean = {
    headers.get("alg").flatMap(_.asString).forall(_.equalsIgnoreCase("hs256"))
  }

  override def getAlgorithmHeaders: Map[String, Json] = Map("alg" -> "HS256".asJson)
}
