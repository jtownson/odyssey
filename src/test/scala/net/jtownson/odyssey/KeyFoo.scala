package net.jtownson.odyssey

import java.io.{File, StringReader}
import java.net.URI
import java.nio.file.Paths
import java.security.spec.PKCS8EncodedKeySpec
import java.security.{KeyFactory, KeyPair, PrivateKey, PublicKey}
import java.util.Base64

import net.jtownson.odyssey.impl.Using
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.bouncycastle.openssl.{PEMKeyPair, PEMParser}

import scala.io.Source

object KeyFoo {

  def getECKeyPair: (URI, PrivateKey) = {
    getKeyPair("src/test/resources/id_ecdsa.pem")
  }

  def getEDKeyPair: (URI, Ed25519PrivateKeyParameters) = {
    val str = getKey(Paths.get("src/test/resources/id_ed25519").toFile).trim
    val privateKeyBytes = Base64.getDecoder.decode(str)
    val privateKey = new Ed25519PrivateKeyParameters(privateKeyBytes, 0)
    val publicKeyRef = Paths.get("src/test/resources/id_ed25519.pub").toUri

    (publicKeyRef, privateKey)
  }

  def getEDKeyPairJdk: (URI, PrivateKey) = {
    val str = getKey(Paths.get("src/test/resources/id_ed25519").toFile).trim
    val privateKeyBytes = Base64.getDecoder.decode(str)

    val kf: KeyFactory = KeyFactory.getInstance("ED25519", "BC")
    val ks: PKCS8EncodedKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes)

    val privateKey = kf.generatePrivate(ks)
    val publicKeyRef = Paths.get("src/test/resources/id_ed25519.pub").toUri

    (publicKeyRef, privateKey)
  }

  def getPublicKeyFromRef(publicKeyRef: URI): PublicKey = {
    getKeyPair(new File(publicKeyRef)).getPublic
  }

  private def getKeyPair(key: String): (URI, PrivateKey) = {
    val keyFile = Paths.get(key).toFile
    val publicKeyURI = keyFile.toURI

    (publicKeyURI, getKeyPair(keyFile).getPrivate)
  }

  private def getKey(file: File): String = {
    Using(Source.fromFile(file, "UTF-8"))(_.mkString).get
  }

  private def getKeyPair(file: File): KeyPair = {
    parseKeyPair(getKey(file))
  }

  private def parseKeyPair(key: String): KeyPair = {
    val keyReader = new StringReader(key)
    val parser = new PEMParser(keyReader)
    val pemPair = parser.readObject().asInstanceOf[PEMKeyPair]
    new JcaPEMKeyConverter().getKeyPair(pemPair)
  }
}
