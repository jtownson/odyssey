package net.jtownson.odyssey

import java.net.URI
import java.time.LocalDateTime

import io.circe.JsonObject
import net.jtownson.odyssey.VCBuilder.LinkedDatasetField.EmptyField

trait VC {
  def id: Option[String]
  def issuer: URI
  def issuanceDate: Option[LocalDateTime]
  def expirationDate: Option[LocalDateTime]
  def types: Seq[String]
  def contexts: Seq[URI]
  // ... plus other data model fields defined in the spec

  def claims: Seq[JsonObject]
}

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
  case class SerializedJwsVC(
      id: Option[String],
      issuer: URI,
      issuanceDate: Option[LocalDateTime],
      expirationDate: Option[LocalDateTime],
      types: Seq[String],
      contexts: Seq[URI],
      claims: Seq[JsonObject],
      jws: String
  ) extends VC

  case class ParsedVc private[odyssey] (
      id: Option[String],
      issuer: URI,
      issuanceDate: Option[LocalDateTime],
      expirationDate: Option[LocalDateTime],
      types: Seq[String],
      contexts: Seq[URI],
      claims: Seq[JsonObject]
  ) extends VC

  def apply(): VCBuilder[EmptyField] = VCBuilder()
  def fromJws(jwsSer: String): Either[VerificationError, VC] = JwsCodec.decodeJws(jwsSer)
  def fromJsonLd(jsonLdSer: String): Either[VerificationError, VC] = JsonCodec.decodeJsonLd(jsonLdSer)
}
