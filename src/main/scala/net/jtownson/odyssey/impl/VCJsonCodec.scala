package net.jtownson.odyssey.impl

import java.net.URI
import java.time.LocalDateTime

import io.circe.Json.obj
import io.circe._
import io.circe.parser.decode
import io.circe.syntax._
import net.jtownson.odyssey.VerificationError.ParseError
import net.jtownson.odyssey.impl.ContextValidation.contextDecoder
import net.jtownson.odyssey.impl.IssuerValidation.issuerDecoder
import net.jtownson.odyssey.impl.TypeValidation.typeDecoder
import net.jtownson.odyssey.{VC, VerificationError}

/**
  * Circe encoder/decoder to write verifiable credential data model.
  */
object VCJsonCodec {

  import CodecStuff._

  def decodeJsonLd(jsonLdSer: String): Either[VerificationError, VC] = {
    decode(jsonLdSer)(vcJsonDecoder).left.map(err => ParseError(err.getMessage))
  }

  def vcJsonEncoder: Encoder[VC] = {
    Encoder.instance { vc: VC =>
      obj(
        "@context" -> strOrArr(vc.contexts),
        "id" -> vc.id.map(_.asJson).getOrElse(Json.Null),
        "type" -> vc.types.asJson,
        "issuer" -> vc.issuer.asJson,
        "issuanceDate" -> vc.issuanceDate.asJson,
        "expirationDate" -> vc.expirationDate.map(ldt => ldt.asJson).getOrElse(Json.Null),
        "credentialSubject" -> strOrArr(vc.subjects)
      ).dropNullValues
    }
  }

  def vcJsonDecoder: Decoder[VC] = {
    Decoder.instance { hc: HCursor =>
      for {
        id <- hc.downField("id").as[Option[String]]
        types <- hc.downField("type").as[Seq[String]](typeDecoder("VerifiableCredential"))
        contexts <- hc.downField("@context").as[Seq[URI]](contextDecoder)
        issuer <- hc.downField("issuer").as[Json](issuerDecoder)
        issuanceDate <- hc.downField("issuanceDate").as[LocalDateTime]
        expirationDate <- hc.downField("expirationDate").as[Option[LocalDateTime]]
        credentialSubject <- hc.downField("credentialSubject").as[Json]
      } yield {
        val subject: Seq[JsonObject] = foldCredentialSubject(credentialSubject)
        VC(id, issuer, issuanceDate, expirationDate, types, contexts, subject)
      }
    }
  }

  private def strOrArr[T: Encoder](v: Seq[T]): Json = {
    if (v.length == 1) v.head.asJson else v.asJson
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