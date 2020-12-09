package net.jtownson.odyssey

import java.security.Security
import java.time.LocalDate

import io.circe.Printer
import net.jtownson.odyssey.impl.VCJsonCodec.vcJsonEncoder
import net.jtownson.odyssey.proof.jws.{Es256kJwsSigner, Es256kJwsVerifier}
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.scalatest.FlatSpec
import org.scalatest.concurrent.ScalaFutures._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class VerificationAndSigningSketch extends FlatSpec {

  Security.addProvider(new BouncyCastleProvider())

  implicit val patienceConfig: PatienceConfig = PatienceConfig(timeout = 1 second)

  import syntax._

  val (privateKey, publicKey, publicKeyId, publicKeyResolver) = KeyFoo.generateECKeyPair()

  val vc = VC()
    .withAdditionalType("AddressCredential")
    .withAdditionalContext("https://www.w3.org/2018/credentials/examples/v1")
    .withId("https://www.postoffice.co.uk/addresses/1234")
    .withIssuer("https://www.postoffice.co.uk")
    .withIssuerAttributes("contact" -> "https://www.postoffice.co.uk/contact-us")
    .withIssuanceDate(LocalDate.of(2020, 1, 1).atStartOfDay())
    .withExpirationDate(LocalDate.of(2021, 1, 1).atStartOfDay())
    .withSubjectAttributes(
      "id" -> "did:example:abc123",
      "name" -> "Her Majesty The Queen",
      "jobTitle" -> "Queen",
      "address" -> "Buckingham Palace, SW1A 1AA"
    )

  val signer = new Es256kJwsSigner(publicKeyId, privateKey)

  // To send it somewhere else, we will serialize
  // to JWS...
  val jws: String = vc.toJws(signer, Printer.spaces2).futureValue.compactSerialization

  println("Generated JWS for wire transfer: ")
  println(jws)

  // ... somewhere else, another app, another part of the system, we obtain the jws...
  val verifier = new Es256kJwsVerifier(publicKeyResolver)

  val parseResult: VCDataModel = VCDataModel.fromJws(verifier, jws).futureValue

  println(s"Received dataset has a valid signature and decodes to the following dataset:")
  println(vcJsonEncoder(parseResult).printWith(Printer.spaces2))
}
