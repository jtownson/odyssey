package net.jtownson.odyssey

import java.net.URI
import java.net.URI.create

import net.jtownson.odyssey.VPBuilder.VPField.EmptyField

import scala.concurrent.{ExecutionContext, Future}

case class VP private (
    contexts: Seq[URI],
    id: Option[String],
    types: Seq[String],
    verifiableCredentials: Seq[VC],
    holder: Option[URI]
)

object VP {
  def apply(): VPBuilder[EmptyField, VCBuilder.VCField.EmptyField] = VPBuilder()

  def apply(
      additionalContexts: Seq[URI],
      id: Option[String],
      additionalTypes: Seq[String],
      verifiableCredentials: Seq[VC],
      holder: Option[URI]
  ): VP = {
    new VP(
      contexts = create("https://www.w3.org/2018/credentials/v1") +: additionalContexts,
      id = id,
      types = "VerifiablePresentation" +: additionalTypes,
      verifiableCredentials = verifiableCredentials,
      holder = holder
    )
  }

  def fromJwsCompactSer(verifier: Verifier, jwsSer: String)(implicit
      ec: ExecutionContext
  ): Future[VP] =
    VPJwsCodec.fromJwsCompactSer(verifier, jwsSer)

  def fromJsonLd(jsonLdSer: String): Either[VerificationError, VP] =
    VPJsonCodec.decodeJsonLd(jsonLdSer)
}
