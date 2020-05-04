package net.jtownson.odyssey

import io.circe.Decoder.Result
import io.circe.{Decoder, DecodingFailure, HCursor, Json}

object TypeValidation {
  def typeDecoder(expected: String): Decoder[Seq[String]] =
    (hc: HCursor) => {
      hc.value.fold(
        jsonNull = Left(
          DecodingFailure(
            s"A JSON null is not a valid type definition. Require an array [$expected, ...].",
            hc.history
          )
        ),
        jsonBoolean = _ =>
          Left(
            DecodingFailure(
              s"A JSON boolean is not a valid type definition. Require an array [$expected, ...].",
              hc.history
            )
          ),
        jsonNumber = _ =>
          Left(
            DecodingFailure(
              s"A JSON number is not a valid type definition. Require an array [$expected, ...].",
              hc.history
            )
          ),
        jsonString = _ =>
          Left(
            DecodingFailure(
              s"A JSON string is not a valid type definition. Require an array [$expected, ...].",
              hc.history
            )
          ),
        jsonArray = (s: Seq[Json]) => decodeTypeAsArray(expected, s),
        jsonObject = _ => Left(DecodingFailure("object is not a valid type definition", hc.history))
      )
    }

  // strips off the leading VerifiableCredential or VerifiablePresentation type,
  // since these are fixed and hardcoded in the data model apply methods.
  private def decodeTypeAsArray(expected: String, s: Seq[Json]): Result[Seq[String]] = {
    if (s.isEmpty) {
      Left(DecodingFailure("A type def when an array, cannot be empty.", List()))
    } else if (s.length == 1) {
      validatedHead(expected, s)
    } else {
      for {
        _ <- validatedHead(expected, s)
        tail <- validatedTail(s.tail)
      } yield tail
    }
  }

  private def validatedHead(expected: String, s: Seq[Json]): Either[DecodingFailure, Seq[String]] = {
    for {
      headString <- s.head.as[String]
      headValid <- Either.cond(
        headString == expected,
        Seq.empty,
        DecodingFailure(s"A type def when a single element array, must be [$expected]", List())
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
