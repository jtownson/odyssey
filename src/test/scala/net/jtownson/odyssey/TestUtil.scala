package net.jtownson.odyssey

import java.security.Security
import java.time.LocalDate
import java.util.Base64

import io.circe.{ACursor, Decoder, HCursor}
import net.jtownson.odyssey.proof.jws.{Es256kJwsSigner, Es256kJwsVerifier, HmacSha256JwsSigner}
import net.jtownson.odyssey.syntax._
import org.bouncycastle.jce.provider.BouncyCastleProvider

import scala.concurrent.ExecutionContext.Implicits.global

object TestUtil {

  Security.addProvider(new BouncyCastleProvider)

  val (ecPrivateKey, ecPublicKey, ecPublicKeyId, ecPublicKeyResolver) = KeyFoo.generateECKeyPair()
  val ecJwsSigner = new Es256kJwsSigner(ecPublicKeyId, ecPrivateKey)
  val ecJwsVerifier = new Es256kJwsVerifier(ecPublicKeyResolver)

  val hmacSecret: Array[Byte] = Base64.getUrlDecoder.decode(
    "AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow"
  )
  val hmacSigner = new HmacSha256JwsSigner(hmacSecret)

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

  val aPresentation = VP().withVC(aCredential)

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
