package net.jtownson.odyssey

import io.circe.DecodingFailure
import io.circe.syntax._
import org.scalatest.FlatSpec
import org.scalatest.Matchers._

class TypeValidationSpec extends FlatSpec {

  "TypeValidation" should "error for a single invalid string" in {
    val tpe = "Foo".asJson
    tpe.as[Seq[String]](TypeValidation.typeDecoder) shouldBe Left(
      DecodingFailure(
        "A type def when a single element array, must be [\"VerifiableCredential\"]",
        List()
      )
    )
  }
}
