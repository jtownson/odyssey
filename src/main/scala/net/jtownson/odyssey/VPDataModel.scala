package net.jtownson.odyssey

import java.net.URI
import java.net.URI.create

import net.jtownson.odyssey.impl.{VPJsonCodec, VPJwsCodec}

import scala.concurrent.{ExecutionContext, Future}

case class VPDataModel private (
    contexts: Seq[URI],
    id: Option[String],
    types: Seq[String],
    verifiableCredentials: Seq[VCDataModel],
    holder: Option[URI]
)

object VPDataModel {
  def apply(
      additionalContexts: Seq[URI],
      id: Option[String],
      additionalTypes: Seq[String],
      verifiableCredentials: Seq[VCDataModel],
      holder: Option[URI]
  ): VPDataModel = {
    new VPDataModel(
      contexts = create("https://www.w3.org/2018/credentials/v1") +: additionalContexts,
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
