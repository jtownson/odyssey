package net.jtownson.odyssey

import java.util.Base64

import javax.crypto.spec.SecretKeySpec
import net.jtownson.odyssey.JwsSpec.{device, secret}
import net.jtownson.odyssey.Signer.HMACSha256Signer
import org.jose4j.jws.JsonWebSignature
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import io.circe._
import io.circe.syntax._

class JwsSpec extends FlatSpec {

  "Jws" should "interoperate with jose4j" in {
    val jws = Jws()
      .withJsonPrinter(Printer.noSpaces)
      .withJsonPayload(Json.obj("key" -> "value".asJson))
      .withSigner(device)
      .compactSerializion

    val jose4jJws = new JsonWebSignature()
    jose4jJws.setCompactSerialization(jws)
    jose4jJws.setKey(new SecretKeySpec(secret, "HmacSHA256"))

    jose4jJws.verifySignature() shouldBe true
    jose4jJws.getHeader("alg") shouldBe "HS256"
    jose4jJws.getPayload shouldBe """{"key":"value"}"""
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

    Jws.signingInput(protectedHeader, payload).deep shouldBe Array[Byte](101, 121, 74, 48, 101, 88, 65, 105, 79, 105,
      74, 75, 86, 49, 81, 105, 76, 65, 48, 75, 73, 67, 74, 104, 98, 71, 99, 105, 79, 105, 74, 73, 85, 122, 73, 49, 78,
      105, 74, 57, 46, 101, 121, 74, 112, 99, 51, 77, 105, 79, 105, 74, 113, 98, 50, 85, 105, 76, 65, 48, 75, 73, 67,
      74, 108, 101, 72, 65, 105, 79, 106, 69, 122, 77, 68, 65, 52, 77, 84, 107, 122, 79, 68, 65, 115, 68, 81, 111, 103,
      73, 109, 104, 48, 100, 72, 65, 54, 76, 121, 57, 108, 101, 71, 70, 116, 99, 71, 120, 108, 76, 109, 78, 118, 98, 83,
      57, 112, 99, 49, 57, 121, 98, 50, 57, 48, 73, 106, 112, 48, 99, 110, 86, 108, 102, 81).deep

    device.sign(Jws.signingInput(protectedHeader, payload)).deep shouldBe
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
      ).deep

    Jws.sign(protectedHeader, payload, device).deep shouldBe Array[Byte](
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
    ).deep

    Jws.compactSerialization(
      protectedHeader,
      payload,
      device
    ) shouldBe "eyJ0eXAiOiJKV1QiLA0KICJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ.dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
  }
}

object JwsSpec {
  val secret = Base64.getUrlDecoder.decode(
    "AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow"
  )
  val device = new HMACSha256Signer(secret)
}
