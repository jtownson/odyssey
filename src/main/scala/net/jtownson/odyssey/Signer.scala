package net.jtownson.odyssey

import java.net.URI
import java.security.{PrivateKey, Signature}

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import net.jtownson.odyssey.Jws.JwsField

import scala.concurrent.Future

trait Signer {
  def setHeaderParameters[F <: JwsField](jws: Jws[F]): Jws[F]
  def sign(data: Array[Byte]): Future[Array[Byte]]
}

object Signer {

  // Example asymmetric signer
  class Es256Signer(publicKeyRef: URI, privateKey: PrivateKey) extends Signer {

    private val kid = publicKeyRef.toString

    override def sign(data: Array[Byte]): Future[Array[Byte]] =
      Future.successful {
        val ecdsa: Signature = Signature.getInstance("SHA256withECDSA")
        ecdsa.initSign(privateKey)
        ecdsa.update(data)
        ecdsa.sign()
      }

    override def setHeaderParameters[F <: JwsField](jws: Jws[F]): Jws[F] = {
      jws
        .withHeader("alg", "ES256K")
        .withHeader("kid", kid)
    }
  }

  // Example symmetric signer
  class HmacSha256Signer(secret: Array[Byte]) extends Signer {

    private val key = new SecretKeySpec(secret, "HmacSHA256")

    override def sign(data: Array[Byte]): Future[Array[Byte]] =
      Future.successful {
        val shahmac: Mac = Mac.getInstance("HmacSHA256")
        shahmac.init(key)
        shahmac.doFinal(data)
      }

    override def setHeaderParameters[F <: JwsField](jws: Jws[F]): Jws[F] = {
      jws.withHeader("alg", "HS256")
    }
  }
}
