package net.jtownson.odyssey

import java.io.{File, FileOutputStream, FileWriter, StringWriter}
import java.net.URI
import java.security.spec.X509EncodedKeySpec
import java.security.{KeyPair, KeyPairGenerator, PublicKey, Security}
import java.util.Base64

import io.circe.Printer
import net.jtownson.odyssey.RDFNode.Literal
import net.jtownson.odyssey.VerificationAndSigningSketch.jsonPrinter
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.MiscPEMGenerator
import org.bouncycastle.util.io.pem.{PemObjectGenerator, PemWriter}

import scala.concurrent.ExecutionContext.Implicits.global
import org.scalatest.FlatSpec

import scala.util.{Failure, Success}

class VerificationAndSigningSketch extends FlatSpec {

  import syntax._

  // Insert support for DID registration...

  // Here is a subject, about which we wish to make some statements
  val subject = new URI("did:ata:abc123")

  // Define those statements...
  val context = Context.withNamespaces("f" -> "http://xmlns.com/foaf/0.1/")
  val claims = LinkedDataset
    .withContext(context)
    .withClaims(
      (subject, "f:name", Literal("Her Majesty The Queen")),
      (subject, "f:jobTitle", Literal("Queen")),
      (subject, "f:address", Literal("Buckingham Palace, SW1A 1AA"))
    )

  println(claims.toJson.printWith(jsonPrinter))

  // Define the signature.
  val keyPair = VerificationAndSigningSketch.generateKeypair

  // Can could use HTTPS as the PKI...
  val publicKeyRefHTTPS = "https://example.com/keys/key-1.pem"
  // Or cardano...
  val publicKeyRefDID = "did:ata:<issuer-suffix>#keys-1"

  // but use file system for now
  val publicKeyRefFile = VerificationAndSigningSketch.writeKey(keyPair.getPublic).toURI.toURL
  println(publicKeyRefFile)

  val verifiableClaims = claims.withEd25519Signature2018(keyPair.getPrivate, publicKeyRefFile)

  // To send it somewhere else, we will serialize
  // to json-ld...
  val json: Json = verifiableClaims.toJson

  // ...or some binary encoding
  val proto: Array[Byte] = verifiableClaims.toProto

  /// ... somewhere else, another app, another part of the system, we obtain the json/proto...
  val parseF = LinkedDataset.fromJson(json)

  parseF.onComplete {
    case Success(linkedDataset) =>
    // great, we've got our data back
    case Failure(exception) =>
    // oh dear, there must have been either:
    // a (network) problem resolving the verification public key
    // an invalid JSON object
    // an invalid signature
  }

  /**
  * To implement this API, these steps require a few algorithms:
  * 1. JSON-LD serialization and signing
  * To sign the document it must be 'normalized' so that key ordering,
  * whitespace, etc, do not affect the signature value.
  */

}

object VerificationAndSigningSketch {
  val jsonPrinter = Printer.spaces2

  def generateKeypair: KeyPair = {
    Security.addProvider(new BouncyCastleProvider())
    val generator = KeyPairGenerator.getInstance("Ed25519")
    generator.generateKeyPair()
  }

  def writeKey(publicKey: PublicKey): File = {
    writeFile("id_ed25519.pub", Base64.getEncoder.encode(publicKey.getEncoded))
  }

  def writeFile(filename: String, arr: Array[Byte]): File = {
    val file = new File(filename)
    val bw = new FileOutputStream(file)
    try {
      bw.write(arr)
      file
    } finally {
      bw.close()
    }
  }
}
