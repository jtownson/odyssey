package net.jtownson.odyssey

import java.net.URL
import java.security.{PrivateKey, Signature}

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import net.jtownson.odyssey.Jws.JwsField

trait Signer {
  def setHeaderParameters[F <: JwsField](jws: Jws[F]): Jws[F]
  def sign(data: Array[Byte]): Array[Byte]
}

object Signer {

  // Example asymmetric
  class ES256Signer(publicKeyRef: URL, privateKey: PrivateKey) extends Signer {

    private val kid = publicKeyRef.toString

    override def sign(data: Array[Byte]): Array[Byte] = {
      val ecdsa: Signature = Signature.getInstance("SHA256withECDSA")
      ecdsa.initSign(privateKey)
      ecdsa.update(data)
      ecdsa.sign()
    }

    override def setHeaderParameters[F <: JwsField](jws: Jws[F]): Jws[F] = {
      jws
        .withHeader("alg", "ES256")
        .withHeader("kid", kid)
    }
  }

  // Example symmetric signature
  class HMACSha256Signer(secret: Array[Byte]) extends Signer {

    private val key = new SecretKeySpec(secret, "HmacSHA256")

    override def sign(data: Array[Byte]): Array[Byte] = {
      val shahmac: Mac = Mac.getInstance("HmacSHA256")
      shahmac.init(key)
      shahmac.doFinal(data)
    }

    override def setHeaderParameters[F <: JwsField](jws: Jws[F]): Jws[F] = {
      jws.withHeader("alg", "HS256")
    }
  }
}
