package net.jtownson.odyssey

import java.net.URI
import java.security.PrivateKey
import java.time.LocalDate
import java.util.Base64

import net.jtownson.odyssey.Signer.{Es256Signer, HmacSha256Signer}
import net.jtownson.odyssey.Verifier.{Es256Verifier, HmacSha256Verifier}
import net.jtownson.odyssey.syntax._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object TestUtil {

  val (publicKeyRef, privateKey): (URI, PrivateKey) = KeyFoo.getKeyPair

  val secret = Base64.getUrlDecoder.decode(
    "AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow"
  )

  val testKeyResolver: PublicKeyResolver = { (publicKeyRef: URI) =>
    Future.successful(KeyFoo.getPublicKeyFromRef(publicKeyRef))
  }

  val hmacSigner = new HmacSha256Signer(secret)
  val hmacVerifier = new HmacSha256Verifier(secret)

  val es256Signer = new Es256Signer(TestUtil.publicKeyRef, TestUtil.privateKey)
  val es256Verifier = new Es256Verifier(TestUtil.testKeyResolver)

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
    .withSigner(es256Signer)

  val aPresentation = VP()
    .withVC(aCredential)
    .withSigner(es256Signer)

}
