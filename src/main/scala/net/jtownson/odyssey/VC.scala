package net.jtownson.odyssey

import java.net.URI
import java.time.LocalDateTime

import io.circe.JsonObject
import net.jtownson.odyssey.VCBuilder.MandatoryField.EmptyField

import scala.concurrent.{ExecutionContext, Future}

case class VC(
    id: Option[String],
    issuer: URI,
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

  def fromJws(algoWhitelist: Seq[String], publicKeyResolver: PublicKeyResolver, jwsSer: String)(
      implicit ec: ExecutionContext
  ): Future[VC] =
    JwsCodec.decodeJws(algoWhitelist, publicKeyResolver, jwsSer)

  def fromJsonLd(jsonLdSer: String): Either[VerificationError, VC] =
    VCJsonCodec.decodeJsonLd(jsonLdSer)
}
