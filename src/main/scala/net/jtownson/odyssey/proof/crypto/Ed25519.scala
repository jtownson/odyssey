package net.jtownson.odyssey.proof.crypto

import java.security.{PrivateKey, PublicKey}

import org.bouncycastle.crypto.params.{Ed25519PrivateKeyParameters, Ed25519PublicKeyParameters}
import org.bouncycastle.crypto.signers.Ed25519Signer

object Ed25519 {

  def sign(data: Array[Byte], privateKeyParameters: Ed25519PrivateKeyParameters): Array[Byte] = {
    val signer = new Ed25519Signer()
    signer.init(true, privateKeyParameters)
    signer.update(data, 0, data.length)
    signer.generateSignature()
  }

  def sign(data: Array[Byte], privateKey: PrivateKey): Array[Byte] = {
    sign(data, new Ed25519PrivateKeyParameters(privateKey.getEncoded, 0))
  }

  def sign(data: Array[Byte], privateKey: Array[Byte]): Array[Byte] = {
    sign(data, new Ed25519PrivateKeyParameters(privateKey, 0))
  }

  def verify(data: Array[Byte], signature: Array[Byte], publicKeyParameters: Ed25519PublicKeyParameters): Boolean = {
    val signer = new Ed25519Signer()
    signer.init(false, publicKeyParameters)
    signer.update(data, 0, data.length)
    signer.verifySignature(signature)
  }

  def verify(data: Array[Byte], signature: Array[Byte], publicKey: PublicKey): Boolean = {
    verify(data, signature, new Ed25519PublicKeyParameters(publicKey.getEncoded, 0))
  }

  def verify(data: Array[Byte], signature: Array[Byte], publicKey: Array[Byte]): Boolean = {
    verify(data, signature, new Ed25519PublicKeyParameters(publicKey, 0))
  }
}
