package net.jtownson.odyssey

import java.io.{File, StringReader}
import java.net.URI
import java.nio.file.Paths
import java.security.{KeyPair, PrivateKey, PublicKey}

import net.jtownson.odyssey.impl.Using
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.bouncycastle.openssl.{PEMKeyPair, PEMParser}

import scala.io.Source

object KeyFoo {

  def getKeyPair: (URI, PrivateKey) = {
    val keyFile = Paths.get("src/test/resources/id_ecdsa.pem").toFile
    val publicKeyURI = keyFile.toURI

    (publicKeyURI, getKeyPair(keyFile).getPrivate)
  }

  def getPublicKeyFromRef(publicKeyRef: URI): PublicKey = {
    getKeyPair(new File(publicKeyRef)).getPublic
  }

  private def getKey(file: File): String = {
    Using(Source.fromFile(file, "UTF-8"))(_.mkString).get
  }

  private def getKeyPair(file: File): KeyPair = {
    getKeyPair(getKey(file))
  }

  private def getKeyPair(key: String): KeyPair = {
    val keyReader = new StringReader(key)
    val parser = new PEMParser(keyReader)
    val pemPair = parser.readObject().asInstanceOf[PEMKeyPair]
    new JcaPEMKeyConverter().getKeyPair(pemPair);
  }
}
