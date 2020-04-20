package net.jtownson.odyssey

import java.net.URI
import java.time.LocalDateTime

import io.circe.JsonObject
import net.jtownson.odyssey.VCBuilder.LinkedDatasetField.EmptyField

case class VC(
    id: Option[String],
    issuer: URI,
    issuanceDate: Option[LocalDateTime],
    expirationDate: Option[LocalDateTime],
    types: Seq[String],
    contexts: Seq[URI],
    claims: Seq[JsonObject]
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
  def fromJws(jwsSer: String): Either[VerificationError, VC] = JwsCodec.decodeJws(jwsSer)
  def fromJsonLd(jsonLdSer: String): Either[VerificationError, VC] = JsonCodec.decodeJsonLd(jsonLdSer)
}
