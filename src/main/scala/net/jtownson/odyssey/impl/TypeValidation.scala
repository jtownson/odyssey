package net.jtownson.odyssey.impl

import io.circe.Decoder.Result
import io.circe.{Decoder, DecodingFailure, HCursor, Json}

object TypeValidation {

  val failureMessage =
    """
      |Sec 4.3: The value of the type property MUST be,
      |or map to (through interpretation of the @context property),
      |one or more URIs.""".stripMargin

  // TODO this needs to be Decoder[Seq[URI]]
  def typeDecoder(expected: String): Decoder[Seq[String]] =
    (hc: HCursor) => {
      lazy val fail = DecodingFailure(failureMessage, hc.history)
      lazy val failResult: Decoder.Result[Seq[String]] = Left(fail)

      hc.value.fold(
        jsonNull = failResult,
        jsonBoolean = _ => failResult,
        jsonNumber = _ => failResult,
        jsonString = s => Right(Seq.empty), // TODO
        jsonArray = a => decodeTypeAsArray(expected, a),
        jsonObject = _ => failResult
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
