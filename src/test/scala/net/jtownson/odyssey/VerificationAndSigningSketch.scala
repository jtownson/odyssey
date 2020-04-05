package net.jtownson.odyssey

import java.net.URI

import net.jtownson.odyssey.RDFNode.Literal
import org.scalatest.FlatSpec

class VerificationAndSigningSketch extends FlatSpec {

  import syntax._

  // Insert support for DID registration...

  // Here is a subject, about which we wish to make some statements
  val subject = new URI("did:ata:abc123")

  // Define those statements...
  val claims = LinkedDataset
    .withNamespaces("f" -> "http://xmlns.com/foaf/0.1/")
    .withStatements(
      (subject, "f:name", Literal("Her Majesty The Queen")),
      (subject, "f:jobTitle", Literal("Queen")),
      (subject, "f:address", Literal("Buckingham Palace, SW1A 1AA"))
    )

  // Define the signature.
  // For now we use public keys on disk,
  // so 'resolving' the key is just loading it from the openssl pem file.
  val (publicKeyRef, privateKey) = KeyFoo.getKeyPair
  println(s"The public key reference for verification is $publicKeyRef")
  // Real code could use HTTPS...
  val publicKeyRefHTTPS = "https://<host>/<path>/<key>"
  // Or DIDs...
  val publicKeyRefDID = "did:<method>:<issuer-suffix>#<key>"

  val verifiableClaims = claims.withEcdsaSecp256k1Signature2019(publicKeyRef, privateKey)

  // To send it somewhere else, we will serialize
  // to JWS...
  val jws: String = verifiableClaims.toJWS

  println("Generated JWS for wire transfer: ")
  println(jws)

  /// ... somewhere else, another app, another part of the system, we obtain the json/proto...
  val parseE = LinkedDataset.fromJws(jws)

  parseE match {
    case Right(linkedDataset) =>
      // great, we have our data back
      println("Received dataset has valid signature and decodes to the following dataset: ")
      linkedDataset.rdfModel.write(System.out, "JSON-LD")
    case Left(error) =>
      // oh dear, there must have been either:
      // a (network) problem resolving the verification public key
      // an invalid JSON object
      // an invalid signature
      println(s"Error: $error")
  }
}
