package net.jtownson.odyssey

import java.net.{URI, URL}
import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneId}

import io.circe.Decoder.Result
import io.circe.Json.obj
import io.circe._
import io.circe.parser.decode
import io.circe.syntax._
import net.jtownson.odyssey.JsonCodec.JsonValidation.contextDecoder
import net.jtownson.odyssey.VerificationError.ParseError

import scala.util.{Failure, Success, Try}

/**
  * Circe encoder/decoder to write verifiable credential data model.
  */
object JsonCodec {

  implicit val urlEncoder: Encoder[URL] = Encoder[String].contramap(_.toString)
  implicit val urlDecoder: Decoder[URL] = Decoder[String].map(new URL(_))

  implicit val uriEncoder: Encoder[URI] = Encoder[String].contramap(_.toString)
  implicit val uriDecoder: Decoder[URI] = Decoder[String].map(new URI(_))

  implicit val localDateTimeEncoder: Encoder[LocalDateTime] =
    Encoder[String].contramap(d => dfRfc3339.format(d))
  implicit val localDateTimeDecoder: Decoder[LocalDateTime] =
    Decoder[String].map(dateStr => LocalDateTime.from(dfRfc3339.parse(dateStr)))

  private val dfRfc3339 = DateTimeFormatter
    .ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
    .withZone(ZoneId.of("UTC"))

  def decodeJsonLd(jsonLdSer: String): Either[VerificationError, VC] = {
    decode(jsonLdSer)(vcJsonDecoder).left.map { err =>
      println(err)
      ParseError()
    }
  }

  def vcJsonEncoder: Encoder[VC] = {
    Encoder.instance { vc: VC =>
      obj(
        "@context" -> strOrArr(vc.contexts),
        "id" -> vc.id.map(_.asJson).getOrElse(Json.Null),
        "type" -> strOrArr(vc.types),
        "issuer" -> vc.issuer.asJson,
        "issuanceDate" -> vc.issuanceDate.map(ldt => ldt.asJson).getOrElse(Json.Null),
        "expirationDate" -> vc.expirationDate.map(ldt => ldt.asJson).getOrElse(Json.Null),
        "credentialSubject" -> strOrArr(vc.claims)
      ).dropNullValues
    }
  }

  private def strOrArr[T: Encoder](v: Seq[T]): Json = {
    if (v.length == 1) v.head.asJson else v.asJson
  }

  def vcJsonDecoder: Decoder[VC] = {
    Decoder.instance { hc: HCursor =>
      for {
        id <- hc.downField("id").as[Option[String]]
        types <- hc.downField("type").as[Seq[String]]
        contexts <- hc.downField("@context").as[Seq[URI]](contextDecoder)
        issuer <- hc.downField("issuer").as[URI]
        issuanceDate <- hc.downField("issuanceDate").as[Option[LocalDateTime]]
        expirationDate <- hc.downField("expirationDate").as[Option[LocalDateTime]]
        credentialSubject <- hc.downField("credentialSubject").as[Json]
      } yield {
        val subject: Seq[JsonObject] = foldCredentialSubject(credentialSubject)
        VC(id, issuer, issuanceDate, expirationDate, types, contexts, subject)
      }
    }
  }

  object JsonValidation {

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

  private def foldCredentialSubject(json: Json): Seq[JsonObject] = {
    import io.circe.Decoder.Result
    json.fold(
      Seq.empty,
      _ => Seq.empty,
      _ => Seq.empty,
      _ => Seq.empty,
      arr => arr.flatMap(foldCredentialSubject),
      jsonObject => Seq(jsonObject)
    )
  }
}
