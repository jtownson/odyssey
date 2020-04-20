package net.jtownson.odyssey

import java.net.{URI, URL}
import java.security.PrivateKey
import java.time.LocalDate

import io.circe.Printer
import org.scalatest.FlatSpec

class VerificationAndSigningSketch extends FlatSpec {

  import syntax._

  val (publicKeyRef, privateKey): (URL, PrivateKey) = KeyFoo.getKeyPair

  val vc = VC()
    .withAdditionalType("AddressCredential")
    .withAdditionalContext("https://www.w3.org/2018/credentials/examples/v1")
    .withId("https://www.postoffice.co.uk/addresses/1234")
    .withIssuer("https://www.postoffice.co.uk")
    .withIssuanceDate(LocalDate.of(2020, 1, 1).atStartOfDay())
    .withExpirationDate(LocalDate.of(2021, 1, 1).atStartOfDay())
    .withCredentialSubject(
      ("id", "did:ata:abc123"),
      ("name", "Her Majesty The Queen"),
      ("jobTitle", "Queen"),
      ("address", "Buckingham Palace, SW1A 1AA")
    )
    .withEcdsaSecp256k1Signature2019(publicKeyRef, privateKey)

  println(s"The public key reference for verification is $publicKeyRef")

  // To send it somewhere else, we will serialize
  // to JWS...
  val jws: String = vc.toJws

  println("Generated JWS for wire transfer: ")
  println(jws)

  // ... somewhere else, another app, another part of the system, we obtain the jws...
  val parseE: Either[VerificationError, VC] = VC.fromJws(jws)

  parseE match {
    case Right(vc) =>
      // great, we have our data back
      println(s"Received dataset has valid signature and decodes to the following dataset:")
      println(VcJsonCodec.vcJsonEncoder(vc).printWith(Printer.spaces2))
    case Left(error) =>
      // oh dear, there must have been either:
      // a (network) problem resolving the verification public key
      // an invalid JSON object
      // an invalid signature
      println(s"Error: $error")
  }
}
