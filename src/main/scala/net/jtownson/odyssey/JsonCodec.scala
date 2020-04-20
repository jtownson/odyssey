package net.jtownson.odyssey

import java.net.{URI, URL}
import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneId}

import io.circe.Json.obj
import io.circe._
import io.circe.parser.decode
import io.circe.syntax._
import net.jtownson.odyssey.VerificationError.ParseError

/**
  * Circe encoder/decoder to write verifiable credential data model.
  */
object JsonCodec {

  private implicit val urlEncoder: Encoder[URL] = Encoder[String].contramap(_.toString)
  private implicit val urlDecoder: Decoder[URL] = Decoder[String].map(new URL(_))

  private implicit val uriEncoder: Encoder[URI] = Encoder[String].contramap(_.toString)
  private implicit val uriDecoder: Decoder[URI] = Decoder[String].map(new URI(_))

  private implicit val localDateTimeEncoder: Encoder[LocalDateTime] =
    Encoder[String].contramap(d => dfRfc3339.format(d))
  private implicit val localDateTimeDecoder: Decoder[LocalDateTime] =
    Decoder[String].map(dateStr => LocalDateTime.from(dfRfc3339.parse(dateStr)))

  private val dfRfc3339 = DateTimeFormatter
    .ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
    .withZone(ZoneId.of("UTC"))

  def decodeJsonLd(jsonLdSer: String): Either[VerificationError, VC] = {
    decode(jsonLdSer)(vcJsonDecoder).left.map { err =>
      println(err)
      ParseError
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

  def vcJsonDecoder
      : Decoder[VC] = { // TODO passing valid whitelisted algos plus key resolvers for the pki schemes supported.
    Decoder.instance { hc: HCursor =>
      for {
        id <- hc.downField("id").as[Option[String]]
        types <- hc.downField("type").as[Seq[String]]
        contexts <- hc.downField("@context").as[Seq[URI]]
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

  private def foldCredentialSubject(json: Json): Seq[JsonObject] = {
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
