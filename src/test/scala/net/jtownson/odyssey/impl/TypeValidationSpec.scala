package net.jtownson.odyssey.impl

import io.circe.DecodingFailure
import io.circe.syntax._
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.Inside._
class TypeValidationSpec extends FlatSpec {

  "TypeValidation" should "error for a single invalid string" in {
    val tpe = "Foo".asJson
    inside(tpe.as[Seq[String]](TypeValidation.typeDecoder("VerifiableCredential"))) {
      case Left(DecodingFailure(msg, _)) =>
        msg should startWith("Sec 4.3: The value of the type property")
    }
  }
}
