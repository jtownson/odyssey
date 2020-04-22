package net.jtownson.odyssey

import java.net.URI

import io.circe.Decoder.Result
import io.circe.{Decoder, DecodingFailure, HCursor, Json}

import scala.util.Try

object ContextValidation {

  def contextDecoder: Decoder[Seq[URI]] = (hc: HCursor) => {
    hc.value.fold(
      jsonNull = Left(DecodingFailure("null is r r r wrong", hc.history)),
      jsonBoolean = _ => Left(DecodingFailure("boolean is r r r wrong", hc.history)),
      jsonNumber = _ => Left(DecodingFailure("number is r r r wrong", hc.history)),
      jsonString = s => decodeContextAsString(s),
      jsonArray = (s: Seq[Json]) => decodeContextAsArray(s),
      jsonObject = obj => {
        println(s"expanding obj to empty seq: $obj")
        Right(Seq.empty)
      }
    )
  }

  private def decodeContextAsArray(s: Seq[Json]): Result[Seq[URI]] = {
    if (s.isEmpty) {
      Left(DecodingFailure("A @context when an array, cannot be empty.", List()))
    } else if (s.length == 1) {
      Left(
        DecodingFailure(
          "A @context when an array, must have multiple elements with first element 'https://www.w3.org/2018/credentials/v1'",
          List()
        )
      )
    } else {
      for {
        headString <- s.head.as[String]
        headValid <- decodeContextAsString(headString)
      } yield headValid
    }
  }

  private def decodeContextAsString(s: String): Decoder.Result[Seq[URI]] = {
    Try(new URI(s))
      .filter(_.toString == "https://www.w3.org/2018/credentials/v1")
      .map(Seq(_))
      .toEither
      .left
      .map(
        _ =>
          DecodingFailure(
            "A @context value, when a string, must equal 'https://www.w3.org/2018/credentials/v1'",
            List()
          )
      )
  }
}
