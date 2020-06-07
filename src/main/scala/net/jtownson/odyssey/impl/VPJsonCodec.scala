package net.jtownson.odyssey.impl

import java.net.URI

import io.circe.Json.obj
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.{Decoder, Encoder, HCursor, Json}
import net.jtownson.odyssey.VerificationError.ParseError
import net.jtownson.odyssey.impl.ContextValidation.contextDecoder
import net.jtownson.odyssey.impl.TypeValidation.typeDecoder
import net.jtownson.odyssey.{VC, VP, VerificationError}

/**
  * Circe encoder/decoder to write verifiable presentations.
  */
object VPJsonCodec {

  import CodecStuff._

  implicit val vcEncoder = VCJsonCodec.vcJsonEncoder
  implicit val vcDecoder = VCJsonCodec.vcJsonDecoder

  def decodeJsonLd(jsonLdSer: String): Either[VerificationError, VP] = {
    decode(jsonLdSer)(vpJsonDecoder).left.map(err => ParseError(err.getMessage))
  }

  def vpJsonEncoder: Encoder[VP] = {
    Encoder.instance { vp: VP =>
      obj(
        "@context" -> strOrArr(vp.contexts),
        "id" -> vp.id.map(_.asJson).getOrElse(Json.Null),
        "type" -> vp.types.asJson,
        "verifiableCredential" -> vp.verifiableCredentials.asJson,
        "holder" -> vp.holder.map(_.asJson).getOrElse(Json.Null),
        "proof" -> Seq.empty[String].asJson
      ).dropNullValues
    }
  }

  def vpJsonDecoder: Decoder[VP] = {
    Decoder.instance { hc: HCursor =>
      for {
        id <- hc.downField("id").as[Option[String]]
        types <- hc.downField("type").as[Seq[String]](typeDecoder("VerifiablePresentation"))
        contexts <- hc.downField("@context").as[Seq[URI]](contextDecoder)
        vc <- hc.downField("verifiableCredential").as[Seq[VC]]
        holder <- hc.downField("holder").as[Option[URI]]
        _ <- hc.downField("proof").as[Json]
      } yield {
        VP(additionalContexts = contexts, id = id, additionalTypes = types, vc, holder)
      }
    }
  }

  private def strOrArr[T: Encoder](v: Seq[T]): Json = {
    if (v.length == 1) v.head.asJson else v.asJson
  }
}
