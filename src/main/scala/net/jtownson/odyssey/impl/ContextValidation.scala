package net.jtownson.odyssey.impl

import java.net.URI

import io.circe.Decoder.Result
import io.circe.syntax._
import io.circe.{Decoder, DecodingFailure, HCursor, Json, JsonObject}
import net.jtownson.odyssey.impl.CodecStuff._

import scala.util.Try

object ContextValidation {

  def contextDecoder: Decoder[Seq[Json]] =
    (hc: HCursor) => {
      hc.value.fold(
        jsonNull = Left(DecodingFailure("null is r r r wrong", hc.history)),
        jsonBoolean = _ => Left(DecodingFailure("boolean is r r r wrong", hc.history)),
        jsonNumber = _ => Left(DecodingFailure("number is r r r wrong", hc.history)),
        jsonString = s => decodeContextAsString(s),
        jsonArray = s => decodeContextAsArray(s),
        jsonObject = obj => {
          println(s"expanding obj to empty seq: $obj")
          Right(Seq.empty)
        }
      )
    }

  private def decodeContextAsObject(o: JsonObject): Result[Seq[Json]] = {
    Right(Seq(o.asJson))
  }

  private def decodeContextAsArray(s: Seq[Json]): Result[Seq[Json]] = {
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
        _ <- validatedHead(URI.create("https://www.w3.org/2018/credentials/v1"), s.head)
        tail <- validatedTail(s.tail)
      } yield tail
    }
  }

  private def decodeContextAsString(s: String): Result[Seq[Json]] = {
    Try(new URI(s))
      .filter(_.toString == "https://www.w3.org/2018/credentials/v1")
      .map(_ => Seq.empty)
      .toEither
      .left
      .map(_ =>
        DecodingFailure(
          "A @context value, when a string, must equal 'https://www.w3.org/2018/credentials/v1'",
          List()
        )
      )
  }

  private def validatedHead(expected: URI, h: Json): Either[DecodingFailure, Seq[URI]] = {
    for {
      head <- h.as[URI]
      headValid <- Either.cond(
        head == expected,
        Seq.empty,
        DecodingFailure(s"Expected context value of $expected. Got $head", List())
      )
    } yield headValid
  }

  private def unvalidatedHead(s: Json): Either[DecodingFailure, Seq[Json]] = {
    if (s.isString)
      s.as[URI].map(uri => Seq(uri.asJson))
    else if (s.isObject)
      Right(Seq(s))
    else
      Right(Seq.empty)
  }

  private def validatedTail(s: Seq[Json]): Either[DecodingFailure, Seq[Json]] = {
    if (s.isEmpty) {
      Right(Seq.empty)
    } else {
      for {
        head <- unvalidatedHead(s.head)
        tail <- validatedTail(s.tail)
      } yield head ++ tail
    }
  }
}
