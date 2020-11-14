package net.jtownson.odyssey

import java.net.URI.create
import java.time.LocalDateTime

import io.circe.{Json, JsonObject}
import net.jtownson.odyssey.impl.VCJsonCodec.vcJsonEncoder
import net.jtownson.odyssey.impl.{VCJsonCodec, VCJwsCodec}

import scala.concurrent.{ExecutionContext, Future}

case class VCDataModel private (
    id: Option[String],
    issuer: Json,
    issuanceDate: LocalDateTime,
    expirationDate: Option[LocalDateTime],
    types: Seq[String],
    contexts: Seq[Json],
    subjects: Seq[JsonObject],
    credentialSchemas: Seq[DataSchema],
    dummy: Option[String]
) {
  def toJson: Json = vcJsonEncoder(this)
  def toJws(signer: Signer) = VCJwsCodec.toJws(signer, this)
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
object VCDataModel {
  def apply(
      id: Option[String],
      issuer: Json,
      issuanceDate: LocalDateTime,
      expirationDate: Option[LocalDateTime],
      additionalTypes: Seq[String] = Seq.empty,
      additionalContexts: Seq[Json] = Seq.empty,
      subjects: Seq[JsonObject],
      credentialSchemas: Seq[DataSchema] = Seq.empty
  ): VCDataModel = {
    import impl.CodecStuff._
    import io.circe.syntax._
    new VCDataModel(
      id,
      issuer,
      issuanceDate,
      expirationDate,
      "VerifiableCredential" +: additionalTypes,
      create("https://www.w3.org/2018/credentials/v1").asJson +: additionalContexts.map(_.asJson),
      subjects,
      credentialSchemas,
      None
    )
  }

  def fromJwsCompactSer(verifier: Verifier, jwsSer: String)(implicit ec: ExecutionContext): Future[VCDataModel] =
    VCJwsCodec.fromJwsCompactSer(verifier, jwsSer)

  def fromJsonLd(jsonLdSer: String): Either[VerificationError, VCDataModel] =
    VCJsonCodec.decodeJsonLd(jsonLdSer)
}
