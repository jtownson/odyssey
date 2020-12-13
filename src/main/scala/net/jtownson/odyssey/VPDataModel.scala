package net.jtownson.odyssey

import java.net.URI
import java.net.URI.create

import io.circe.syntax.EncoderOps
import io.circe.{Json, Printer}
import net.jtownson.odyssey.VerificationError.ParseError
import net.jtownson.odyssey.impl.CodecStuff.uriEncoder
import net.jtownson.odyssey.impl.VPJsonCodec
import net.jtownson.odyssey.impl.VPJsonCodec.vpJsonEncoder
import net.jtownson.odyssey.proof.{JwsSigner, JwsVerifier}

import scala.concurrent.{ExecutionContext, Future}

case class VPDataModel private (
    contexts: Seq[Json],
    id: Option[String],
    types: Seq[String],
    verifiableCredentials: Seq[VCDataModel],
    holder: Option[URI]
) {
  def toJson: Json = vpJsonEncoder(this)

  def toJws(signer: JwsSigner, printer: Printer)(implicit ec: ExecutionContext): Future[Jws] =
    JwsSigner.sign(this, printer, signer)
}

object VPDataModel {
  def apply(
      additionalContexts: Seq[Json],
      id: Option[String],
      additionalTypes: Seq[String],
      verifiableCredentials: Seq[VCDataModel],
      holder: Option[URI]
  ): VPDataModel = {
    new VPDataModel(
      contexts = create("https://www.w3.org/2018/credentials/v1").asJson(uriEncoder) +: additionalContexts,
      id = id,
      types = "VerifiablePresentation" +: additionalTypes,
      verifiableCredentials = verifiableCredentials,
      holder = holder
    )
  }

  def fromJws(verifier: JwsVerifier, jwsCompactSer: String)(implicit ec: ExecutionContext): Future[VPDataModel] = {
    JwsVerifier.fromJws[VPDataModel](verifier, jwsCompactSer, VPJsonCodec.vpJsonDecoder, presentationFixup(_))
  }

  def fromJsonLd(jsonLdSer: String): Either[VerificationError, VPDataModel] =
    VPJsonCodec.decodeJsonLd(jsonLdSer)

  private def presentationFixup(payloadJson: Json): Either[ParseError, Json] = {
    payloadJson.hcursor.downField("vp").as[Json].left.map(decodingFailure => ParseError(decodingFailure.message))
  }
}
