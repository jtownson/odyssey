package net.jtownson.odyssey

import java.nio.charset.StandardCharsets.UTF_8
import java.util.Base64

import io.circe.Printer
import io.circe.Printer.spaces2
import io.circe.syntax._
import javax.crypto.spec.SecretKeySpec
import net.jtownson.odyssey.JwsSpec._
import net.jtownson.odyssey.proof.JwsSigner
import net.jtownson.odyssey.proof.jws.{Es256kJwsSigner, Es256kJwsVerifier, HmacSha256JwsSigner}
import org.jose4j.jws.JsonWebSignature
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class JwsSpec extends FlatSpec {

  "Jws" should "interoperate with jose4j" in {
    val headers = Jws.utf8ProtectedHeaders(spaces2, hmacSigner.getAlgorithmHeaders)
    val payload = """{"key":"value"}""".getBytes(UTF_8)

    val signature = hmacSigner.sign(Jws.signingInput(headers, payload)).futureValue
    val jws = Jws(headers, payload, signature).getOrElse(fail("Invalid headers")).compactSerialization

    val jose4jJws = new JsonWebSignature()
    jose4jJws.setCompactSerialization(jws)
    jose4jJws.setKey(new SecretKeySpec(secret, "HmacSHA256"))

    jose4jJws.verifySignature() shouldBe true
    jose4jJws.getHeader("alg") shouldBe "HS256"
    jose4jJws.getPayload shouldBe new String(payload, UTF_8)
  }

  it should "serialize and deserialize a JWS with a signature" in {
    List((hmacSigner, hmacSigner), (es256Signer, es256Verifier)).foreach { sv =>
      val (signer, verifier) = sv
      val protectedHeaders = Map("h" -> "hh".asJson)
      val utf8Headers = """{"h":"hh"}""".getBytes(UTF_8)
      val payload = """{"key":"value"}""".getBytes(UTF_8)
      val jwscs = JwsSigner.sign(protectedHeaders, payload, Printer.spaces2, signer).futureValue.compactSerialization

      val jws = Jws.fromCompactSer(jwscs).getOrElse(fail("Invalid headers"))

      val actualHeaders = jws.protectedHeaders
      actualHeaders("h") shouldBe "hh".asJson
      actualHeaders("alg") shouldBe "HS256".asJson
      jws.payload.toSeq shouldBe payload.toSeq
    }
  }

  it should "be consistent with the example at https://tools.ietf.org/html/rfc7515#appendix-A.1" in {
    val protectedHeader = Array[Byte](123, 34, 116, 121, 112, 34, 58, 34, 74, 87, 84, 34, 44, 13, 10, 32, 34, 97, 108,
      103, 34, 58, 34, 72, 83, 50, 53, 54, 34, 125)

    val payload = Array[Byte](123, 34, 105, 115, 115, 34, 58, 34, 106, 111, 101, 34, 44, 13, 10, 32, 34, 101, 120, 112,
      34, 58, 49, 51, 48, 48, 56, 49, 57, 51, 56, 48, 44, 13, 10, 32, 34, 104, 116, 116, 112, 58, 47, 47, 101, 120, 97,
      109, 112, 108, 101, 46, 99, 111, 109, 47, 105, 115, 95, 114, 111, 111, 116, 34, 58, 116, 114, 117, 101, 125)

    Jws.base64Url(protectedHeader) shouldBe "eyJ0eXAiOiJKV1QiLA0KICJhbGciOiJIUzI1NiJ9"

    Jws.base64Url(
      payload
    ) shouldBe "eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ"

    Jws.signingInput(protectedHeader, payload).toSeq shouldBe Seq[Byte](101, 121, 74, 48, 101, 88, 65, 105, 79, 105, 74,
      75, 86, 49, 81, 105, 76, 65, 48, 75, 73, 67, 74, 104, 98, 71, 99, 105, 79, 105, 74, 73, 85, 122, 73, 49, 78, 105,
      74, 57, 46, 101, 121, 74, 112, 99, 51, 77, 105, 79, 105, 74, 113, 98, 50, 85, 105, 76, 65, 48, 75, 73, 67, 74,
      108, 101, 72, 65, 105, 79, 106, 69, 122, 77, 68, 65, 52, 77, 84, 107, 122, 79, 68, 65, 115, 68, 81, 111, 103, 73,
      109, 104, 48, 100, 72, 65, 54, 76, 121, 57, 108, 101, 71, 70, 116, 99, 71, 120, 108, 76, 109, 78, 118, 98, 83, 57,
      112, 99, 49, 57, 121, 98, 50, 57, 48, 73, 106, 112, 48, 99, 110, 86, 108, 102, 81)

    val signature = hmacSigner.sign(Jws.signingInput(protectedHeader, payload)).futureValue

    signature.toSeq shouldBe
      Array[Byte](
        116,
        24,
        223.toByte,
        180.toByte,
        151.toByte,
        153.toByte,
        224.toByte,
        37,
        79,
        250.toByte,
        96,
        125,
        216.toByte,
        173.toByte,
        187.toByte,
        186.toByte,
        22,
        212.toByte,
        37,
        77,
        105,
        214.toByte,
        191.toByte,
        240.toByte,
        91,
        88,
        5,
        88,
        83,
        132.toByte,
        141.toByte,
        121
      ).toSeq

    Jws(protectedHeader, payload, signature).getOrElse(fail("Invalid headers")).compactSerialization shouldBe
      "eyJ0eXAiOiJKV1QiLA0KICJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ.dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
  }
}

object JwsSpec {
  private val secret = Base64.getUrlDecoder.decode(
    "AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow"
  )

  private val hmacSigner = new HmacSha256JwsSigner(secret)

  private val es256Signer = new Es256kJwsSigner(TestUtil.publicKeyRef, TestUtil.privateKey)
  private val es256Verifier = new Es256kJwsVerifier(TestUtil.testKeyResolver)
}
