package net.jtownson.odyssey.proof.crypto

import java.math.BigInteger
import java.security.Signature
import java.security.interfaces.{ECPrivateKey, ECPublicKey}

object Ecdsa {

  case class EcSignature(r: BigInteger, s: BigInteger)

  private val alg = "SHA256withECDSA"

  def sign(data: Array[Byte], privateKey: ECPrivateKey): Array[Byte] = {
    val signer = Signature.getInstance(alg)
    signer.initSign(privateKey)
    signer.update(data)
    signer.sign()
  }

  def verify(data: Array[Byte], signature: Array[Byte], publicKey: ECPublicKey): Boolean = {
    val signer = Signature.getInstance(alg)
    signer.initVerify(publicKey)
    signer.update(data)
    signer.verify(signature)
  }
}
