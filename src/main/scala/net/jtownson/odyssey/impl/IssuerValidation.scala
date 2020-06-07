package net.jtownson.odyssey.impl

import java.net.URI

import io.circe._

import scala.util.Try
import io.circe.syntax._
import net.jtownson.odyssey.impl.CodecStuff._

object IssuerValidation {

  val failureMessage = "Sec 4.5: the issuer property MUST be either a URI or an object containing an id property."

  def issuerDecoder: Decoder[Json] = { (hc: HCursor) =>
    {
      lazy val fail = DecodingFailure(failureMessage, hc.history)
      lazy val failResult: Decoder.Result[Json] = Left(fail)

      def verifyUri(s: String): Decoder.Result[Json] = {
        Try(new URI(s))
          .filter(_.isAbsolute)
          .toEither
          .left
          .map(_ => fail)
          .map(_ => s.asJson)
      }

      def verifyObject(obj: JsonObject): Decoder.Result[Json] = {
        obj.toMap.get("id").fold(failResult)(json => Right(obj.asJson))
      }

      hc.value.fold(
        jsonNull = failResult,
        jsonBoolean = _ => failResult,
        jsonNumber = _ => failResult,
        jsonString = verifyUri,
        jsonArray = _ => failResult,
        jsonObject = verifyObject
      )
    }
  }

}
