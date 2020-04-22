package net.jtownson.odyssey

import java.net.URI

import io.circe.syntax._
import io.circe.{DecodingFailure, Json}
import net.jtownson.odyssey.ContextValidation.contextDecoder
import org.scalatest.FlatSpec
import org.scalatest.Matchers._

class ContextValidationSpec extends FlatSpec {

  "JsonValidation" should "error for an illegal context URI" in {
    val context = Json.arr("uri:a".asJson)
    context.as[Seq[URI]](contextDecoder) shouldBe Left(
      DecodingFailure(
        "A @context when an array, must have multiple elements with first element 'https://www.w3.org/2018/credentials/v1'",
        List()
      )
    )
  }

  it should "error for a single element array (according to questionable w3c test: MUST be one or more URIs (negative))" in {
    val context = Json.arr("https://www.w3.org/2018/credentials/v1".asJson)
    context.as[Seq[URI]](contextDecoder) shouldBe Left(
      DecodingFailure(
        "A @context when an array, must have multiple elements with first element 'https://www.w3.org/2018/credentials/v1'",
        List()
      )
    )
  }

  it should "error for a single invalid string" in {
    val context = "https://foo.org/bar".asJson
    context.as[Seq[URI]](contextDecoder) shouldBe Left(
      DecodingFailure(
        "A @context value, when a string, must equal 'https://www.w3.org/2018/credentials/v1'",
        List()
      )
    )
  }

  it should "succeed for a single valid string" in {
    val context = "https://www.w3.org/2018/credentials/v1".asJson
    context.as[Seq[URI]](contextDecoder) shouldBe Right(Seq(new URI("https://www.w3.org/2018/credentials/v1")))
  }

  it should "succeed for a mix of URIs and objects" in {
    val context = Json.arr(
      "https://www.w3.org/2018/credentials/v1".asJson,
      Json.obj(
        "image" -> "foo".asJson
      )
    )

    context.as[Seq[URI]](contextDecoder) shouldBe Right(Seq(new URI("https://www.w3.org/2018/credentials/v1")))
  }
}
