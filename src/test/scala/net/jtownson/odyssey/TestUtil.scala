package net.jtownson.odyssey

import java.net.URI
import java.security.PrivateKey
import java.time.LocalDate
import java.util.Base64

import io.circe.{ACursor, Decoder, HCursor}
import net.jtownson.odyssey.proof.jws.{Es256kJwsSigner, Es256kJwsVerifier, HmacSha256JwsSigner}
import net.jtownson.odyssey.syntax._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object TestUtil {

  val (publicKeyRef, privateKey): (URI, PrivateKey) = KeyFoo.getECKeyPair

  val secret = Base64.getUrlDecoder.decode(
    "AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow"
  )

  val testKeyResolver: PublicKeyResolver = { (publicKeyRef: URI) =>
    Future.successful(KeyFoo.getPublicKeyFromRef(publicKeyRef))
  }

  val hmacSigner = new HmacSha256JwsSigner(secret)

  val es256Signer = new Es256kJwsSigner(TestUtil.publicKeyRef, TestUtil.privateKey)
  val es256Verifier = new Es256kJwsVerifier(TestUtil.testKeyResolver)

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

  /**
    * Extract fields from JSON using dot notation names.
    * e.g. {{{ cursor.jsonStr("a.b.c") }}}
    * @param cursor an HCursor
    */
  implicit class CirceFieldAccess(cursor: HCursor) {
    import org.scalatest.OptionValues._
    def jsonStr(dotName: String): String = jsonVal[String](dotName)
    def jsonArr(dotName: String): List[String] = jsonVal[List[String]](dotName)
    def jsonNum[T: Numeric: Decoder](dotName: String): T = jsonVal[T](dotName)

    def jsonVal[T: Decoder](dotName: String): T = {
      @annotation.tailrec
      def loop(l: List[String], ac: ACursor): T = {
        l match {
          case Nil =>
            ac.as[T].toOption.value
          case h :: _ =>
            loop(l.tail, ac.downField(h))
        }
      }
      val l = dotName.split('.').toList
      loop(l.tail, cursor.downField(l.head))
    }
  }
}
