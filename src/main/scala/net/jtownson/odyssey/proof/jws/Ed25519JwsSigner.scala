package net.jtownson.odyssey.proof.jws

import java.net.URI
import java.security.PrivateKey

import io.circe.Json
import io.circe.syntax.EncoderOps
import net.jtownson.odyssey.proof.JwsSigner
import net.jtownson.odyssey.proof.crypto.Ed25519

import scala.concurrent.{ExecutionContext, Future}

class Ed25519JwsSigner(publicKeyRef: URI, privateKey: PrivateKey)(implicit ec: ExecutionContext) extends JwsSigner {

  private val kid = publicKeyRef.toString

  override def sign(data: Array[Byte]): Future[Array[Byte]] =
    Future.successful(signSync(data, privateKey))

  private def signSync(data: Array[Byte], privateKey: PrivateKey): Array[Byte] = {
    Ed25519.sign(data, privateKey)
  }

  override def getAlgorithmHeaders: Map[String, Json] =
    Map("alg" -> "EdDSA".asJson, "crv" -> "Ed25519".asJson, "kid" -> kid.asJson)
}
