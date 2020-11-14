package net.jtownson.odyssey

import java.net.URI
import java.net.URI.create

import io.circe.Json
import io.circe.syntax.EncoderOps
import net.jtownson.odyssey.impl.CodecStuff.uriEncoder
import net.jtownson.odyssey.impl.{VPJsonCodec, VPJwsCodec}

import scala.concurrent.{ExecutionContext, Future}

case class VPDataModel private (
    contexts: Seq[Json],
    id: Option[String],
    types: Seq[String],
    verifiableCredentials: Seq[VCDataModel],
    holder: Option[URI]
)

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

  def fromJwsCompactSer(verifier: Verifier, jwsSer: String)(implicit ec: ExecutionContext): Future[VPDataModel] =
    VPJwsCodec.fromJwsCompactSer(verifier, jwsSer)

  def fromJsonLd(jsonLdSer: String): Either[VerificationError, VPDataModel] =
    VPJsonCodec.decodeJsonLd(jsonLdSer)
}
