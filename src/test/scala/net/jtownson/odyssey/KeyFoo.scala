package net.jtownson.odyssey

import java.net.URI
import java.security.interfaces.{ECPrivateKey, ECPublicKey}
import java.security.spec.ECGenParameterSpec
import java.security.{KeyPairGenerator, PrivateKey, PublicKey}

import net.jtownson.odyssey.VerificationError.PublicKeyResolutionError

import scala.concurrent.Future

object KeyFoo {

  def generateEDKeyPair(): (PrivateKey, PublicKey, URI, PublicKeyResolver) = {
    val generator = KeyPairGenerator.getInstance("Ed25519")
    val keyPair = generator.generateKeyPair()
    val publicKeyId = URI.create("key:ed25519-key-id")
    val publicKey = keyPair.getPublic
    val privateKey = keyPair.getPrivate
    (privateKey, publicKey, publicKeyId, publicKeyResolver(publicKeyId, publicKey))
  }

  def generateECKeyPair(): (ECPrivateKey, ECPublicKey, URI, PublicKeyResolver) = {
    val generator = KeyPairGenerator.getInstance("ECDSA")
    val spec = new ECGenParameterSpec("secp256k1")
    generator.initialize(spec)
    val keyPair = generator.generateKeyPair()
    val ecPublicKey = keyPair.getPublic.asInstanceOf[ECPublicKey]
    val ecPrivateKey = keyPair.getPrivate.asInstanceOf[ECPrivateKey]
    val keyId = URI.create("key:ec-key-id")
    (ecPrivateKey, ecPublicKey, keyId, publicKeyResolver(keyId, ecPublicKey))
  }

  private def publicKeyResolver(publicKeyId: URI, publicKey: PublicKey): PublicKeyResolver = { (publicKeyRef: URI) =>
    if (publicKeyRef == publicKeyId)
      Future.successful(publicKey)
    else
      Future.failed(PublicKeyResolutionError())
  }
}
