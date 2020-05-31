package net.jtownson.odyssey

import java.net.URL
import java.security.PrivateKey
import java.time.LocalDate

import io.circe.Printer
import net.jtownson.odyssey.VerificationAndSigningSketch.{dummyKeyResolver, whitelistedAlgos}
import org.jose4j.jws.AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256
import org.scalatest.FlatSpec
import org.scalatest.concurrent.ScalaFutures._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

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
    .withSubjectAttributes(
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
  val parseResult: VC = VC.fromJws(whitelistedAlgos, dummyKeyResolver, jws).futureValue

  println(s"Received dataset has a valid signature and decodes to the following dataset:")
  println(VCJsonCodec.vcJsonEncoder(parseResult).printWith(Printer.spaces2))
}

object VerificationAndSigningSketch {
  val dummyKeyResolver: PublicKeyResolver = { (publicKeyRef: URL) =>
    Future.successful(KeyFoo.getPublicKeyFromRef(publicKeyRef))
  }
  val whitelistedAlgos = Seq(ECDSA_USING_P256_CURVE_AND_SHA256)
}
