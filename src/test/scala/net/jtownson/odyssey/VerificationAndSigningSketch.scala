package net.jtownson.odyssey

import java.net.URI

import io.circe.Printer
import net.jtownson.odyssey.RDFNode.Literal
import org.scalatest.FlatSpec

import scala.concurrent.ExecutionContext.Implicits.global
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
    .withStatements(
      (subject, "f:name", Literal("Her Majesty The Queen")),
      (subject, "f:jobTitle", Literal("Queen")),
      (subject, "f:address", Literal("Buckingham Palace, SW1A 1AA"))
    )

  // Define the signature.

  // For now we use public keys on disk, so
  // the PKI is the local computer.
  val (publicKeyRef, privateKey) = KeyFoo.getKeyPair
  // Real code could use HTTPS...
  val publicKeyRefHTTPS = "https://example.com/keys/key-1.pem"
  // Or Cardano via prism DIDs...
  val publicKeyRefDID = "did:ata:<issuer-suffix>#keys-1"

  val verifiableClaims = claims.withEcdsaSecp256k1Signature2019(publicKeyRef, privateKey)

  // To send it somewhere else, we will serialize
  // to json-ld...
  val jws: String = verifiableClaims.toJWS

  println(jws)
  // ...or some binary encoding
//  val proto: Array[Byte] = verifiableClaims.toProto

  /// ... somewhere else, another app, another part of the system, we obtain the json/proto...
  val parseE = LinkedDataset.fromJws(jws, KeyFoo.getPublicKeyFromRef(publicKeyRef))

  parseE match {
    case Right(linkedDataset) =>
      // great, we have our data back
      println("got the dataset: ")
      linkedDataset.rdfModel.write(System.out, "JSON-LD")
    case Left(error) =>
      // oh dear, there must have been either:
      // a (network) problem resolving the verification public key
      // an invalid JSON object
      // an invalid signature
      println(s"Error: $error")
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
}
