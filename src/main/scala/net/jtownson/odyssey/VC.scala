package net.jtownson.odyssey

import java.net.URI
import java.net.URI.create
import java.time.LocalDateTime

import io.circe.{Json, JsonObject}
import net.jtownson.odyssey.VCBuilder.VCField.EmptyField

import scala.concurrent.{ExecutionContext, Future}

case class VC private (
    id: Option[String],
    issuer: Json,
    issuanceDate: LocalDateTime,
    expirationDate: Option[LocalDateTime],
    types: Seq[String],
    contexts: Seq[URI],
    subjects: Seq[JsonObject]
)

/**
  * TODO
  * --> arbitrary content with content-type
  * --> resolver configuration
  * ------> uni did resolver resolver
  * ------> https resolver
  * ------> prism-node resolver
  * --> review of claims headers
  * --> canned credentials wrappers (self-signed auth-token, interactive demo creds, ?)
  *
  */
object VC {
  def apply(): VCBuilder[EmptyField] = VCBuilder()

  def apply(
      id: Option[String],
      issuer: Json,
      issuanceDate: LocalDateTime,
      expirationDate: Option[LocalDateTime],
      additionalTypes: Seq[String] = Seq.empty,
      additionalContexts: Seq[URI] = Seq.empty,
      subjects: Seq[JsonObject]
  ): VC = {
    new VC(
      id,
      issuer,
      issuanceDate,
      expirationDate,
      "VerifiableCredential" +: additionalTypes,
      create("https://www.w3.org/2018/credentials/v1") +: additionalContexts,
      subjects
    )
  }

  def fromJws(algoWhitelist: Seq[String], publicKeyResolver: PublicKeyResolver, jwsSer: String)(implicit
      ec: ExecutionContext
  ): Future[VC] =
    VCJwsCodec.decodeJws(algoWhitelist, publicKeyResolver, jwsSer)

  def fromJsonLd(jsonLdSer: String): Either[VerificationError, VC] =
    VCJsonCodec.decodeJsonLd(jsonLdSer)
}
