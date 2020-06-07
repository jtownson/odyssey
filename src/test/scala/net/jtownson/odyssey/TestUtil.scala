package net.jtownson.odyssey

import java.net.URL
import java.security.PrivateKey
import java.time.LocalDate

import org.jose4j.jws.AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256
import syntax._

import scala.concurrent.Future

object TestUtil {
  val (publicKeyRef, privateKey): (URL, PrivateKey) = KeyFoo.getKeyPair

  val testKeyResolver: PublicKeyResolver = { (publicKeyRef: URL) =>
    Future.successful(KeyFoo.getPublicKeyFromRef(publicKeyRef))
  }

  val whitelistedAlgos = Seq(ECDSA_USING_P256_CURVE_AND_SHA256)

  val aCredential = VC()
    .withAdditionalType("AddressCredential")
    .withAdditionalContext("https://www.w3.org/2018/credentials/examples/v1")
    .withId("https://www.postoffice.co.uk/addresses/1234")
    .withIssuer("https://www.postoffice.co.uk")
    .withIssuerAttributes("name" -> "Post Office Ltd.")
    .withIssuanceDate(LocalDate.of(2020, 1, 1).atStartOfDay())
    .withExpirationDate(LocalDate.of(2021, 1, 1).atStartOfDay())
    .withSubjectAttributes(
      ("id", "did:ata:abc123"),
      ("name", "Her Majesty The Queen"),
      ("jobTitle", "Queen"),
      ("address", "Buckingham Palace, SW1A 1AA")
    )
    .withEcdsaSecp256k1Signature2019(publicKeyRef, privateKey)

  val aPresentation = VP()
    .withVC(aCredential)
    .withEcdsaSecp256k1Signature2019(publicKeyRef, privateKey)

}
