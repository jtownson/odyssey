package net.jtownson.odyssey

import io.circe.Decoder.Result
import io.circe.syntax._
import io.circe.{Decoder, DecodingFailure, HCursor, Json}

object TypeValidation {
  def typeDecoder: Decoder[Seq[String]] = (hc: HCursor) => {
    hc.value.fold(
      jsonNull = Left(DecodingFailure("null is not a valid type definition", hc.history)),
      jsonBoolean = _ => Left(DecodingFailure("boolean is not a valid type definition", hc.history)),
      jsonNumber = _ => Left(DecodingFailure("number is not a valid type definition", hc.history)),
      jsonString = s => decodeTypeAsString(s),
      jsonArray = (s: Seq[Json]) => decodeTypeAsArray(s),
      jsonObject = obj => Left(DecodingFailure("object is not a valid type definition", hc.history))
    )
  }

  private def decodeTypeAsString(s: String): Result[Seq[String]] = {
    validatedHead(Seq(s.asJson))
  }

  private def decodeTypeAsArray(s: Seq[Json]): Result[Seq[String]] = {
    if (s.isEmpty) {
      Left(DecodingFailure("A type def when an array, cannot be empty.", List()))
    } else if (s.length == 1) {
      validatedHead(s)
    } else {
      for {
        head <- validatedHead(s)
        tail <- validatedTail(s.tail)
      } yield head ++ tail
    }
  }

  private def validatedHead(s: Seq[Json]): Either[DecodingFailure, Seq[String]] = {
    for {
      headString <- s.head.as[String]
      headValid <- Either.cond(
        headString == "VerifiableCredential",
        Seq(headString),
        DecodingFailure("A type def when a single element array, must be [\"VerifiableCredential\"]", List())
      )
    } yield headValid
  }

  private def validatedTail(s: Seq[Json]): Either[DecodingFailure, Seq[String]] = {
    if (s.isEmpty) {
      Right(Seq.empty)
    } else {
      for {
        head <- s.head.as[String].map(Seq(_))
        tail <- validatedTail(s.tail)
      } yield head ++ tail
    }
  }
}
