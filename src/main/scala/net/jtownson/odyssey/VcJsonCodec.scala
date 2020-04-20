package net.jtownson.odyssey

import java.net.{URI, URL}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import io.circe.Json.obj
import io.circe.syntax._
import io.circe._
import net.jtownson.odyssey.VC.ParsedVc

/**
 * Circe encoder/decoder to write verifiable credential data model.
 */
object VcJsonCodec {

  implicit val urlEncoder: Encoder[URL] = Encoder[String].contramap(_.toString)
  implicit val urlDecoder: Decoder[URL] = Decoder[String].map(new URL(_))

  implicit val uriEncoder: Encoder[URI] = Encoder[String].contramap(_.toString)
  implicit val uriDecoder: Decoder[URI] = Decoder[String].map(new URI(_))

  val df = DateTimeFormatter.ISO_DATE_TIME

  def vcJsonEncoder: Encoder[VC] = {
    Encoder.instance { vc: VC =>
      obj(
        "@context" -> strOrArr(vc.contexts),
        "id" -> vc.id.map(_.asJson).getOrElse(Json.Null),
        "type" -> strOrArr(vc.types),
        "issuer" -> vc.issuer.asJson,
        "issuanceDate" -> vc.issuanceDate.map(ldt => df.format(ldt).asJson).getOrElse(Json.Null),
        "expirationDate" -> vc.expirationDate.map(ldt => df.format(ldt).asJson).getOrElse(Json.Null),
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

        ParsedVc(id, issuer, issuanceDate, expirationDate, types, contexts, subject)
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
