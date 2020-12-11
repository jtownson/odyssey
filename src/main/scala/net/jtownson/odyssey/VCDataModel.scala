package net.jtownson.odyssey

import java.net.URI.create
import java.time.{LocalDateTime, ZoneOffset}

import io.circe.Json.JArray
import io.circe.syntax.EncoderOps
import io.circe.{Json, JsonObject, Printer}
import net.jtownson.odyssey.impl.VCJsonCodec
import net.jtownson.odyssey.impl.VCJsonCodec.vcJsonEncoder
import net.jtownson.odyssey.proof.{JwsSigner, JwsVerifier, LdSigner}

import scala.collection.immutable
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

  def toJson(signer: LdSigner)(implicit ec: ExecutionContext): Future[Json] =
    signer.sign(this)

  def toJws(signer: JwsSigner, printer: Printer)(implicit ec: ExecutionContext): Future[Jws] = {
    val payloadJson = credentialClaims(vcJsonEncoder(this)).asJson
    JwsSigner.sign(credentialHeaders, payloadJson, printer, signer)
  }

  def toJws(signer: LdSigner, printer: Printer)(implicit ec: ExecutionContext): Future[Jws] = {
    signer.sign(this).map { jsonWithProof: Json =>
      val payloadJson = credentialClaims(jsonWithProof).asJson
      val payload = Jws.utf8(printer.print(payloadJson))
      val headers = Jws.utf8ProtectedHeaders(printer, credentialHeaders + ("alg" -> "none".asJson))
      Jws(credentialHeaders, headers, payload, Array.emptyByteArray)
    }
  }

  private val credentialHeaders: Map[String, Json] = {
    Seq(
      Some("cty" -> "application/vc+json".asJson)
//      id.map(id => "jti" -> id.asJson),
//      Some("iss" -> issuer.asJson),
//      Some("nbf" -> issuanceDate.toEpochSecond(ZoneOffset.UTC).asJson),
//      expirationDate.map(exp => "exp" -> exp.toEpochSecond(ZoneOffset.UTC).asJson)
    ).flatten.toMap
  }

  private def credentialClaims(vcJson: Json): Map[String, Json] = {
    Seq(
//      Some("cty" -> "application/vc+json".asJson),
      id.map(id => "jti" -> id.asJson),
      Some("iss" -> issuer.asJson),
      getSubjectJson,
      Some("nbf" -> issuanceDate.toEpochSecond(ZoneOffset.UTC).asJson),
      expirationDate.map(exp => "exp" -> exp.toEpochSecond(ZoneOffset.UTC).asJson),
      Some("vc" -> vcJson)
    ).flatten.toMap
  }

  private def getSubjectJson: Option[(String, Json)] = {
    if (subjects.length == 1) {
      subjects(0)("id").map(subjectId => "sub" -> subjectId)
    } else {
      val subjectValue: immutable.Seq[Json] = subjects
        .foldLeft(Json.arr()) { (acc, nextSubject) =>
          nextSubject("id").fold(acc) { subjectId =>
            (acc.asArray.get :+ subjectId).asJson
          }
        }
        .asArray
        .get
      if (subjectValue.nonEmpty) {
        Some("sub" -> subjectValue.asJson)
      } else {
        None
      }
    }
  }
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

  def fromJsonLd(jsonLdSer: String): Either[VerificationError, VCDataModel] =
    VCJsonCodec.decodeJsonLd(jsonLdSer)

  def fromJws(verifier: JwsVerifier, jwsCompactSer: String)(implicit ec: ExecutionContext): Future[VCDataModel] = {
    JwsVerifier.fromJws[VCDataModel](verifier, jwsCompactSer, VCJsonCodec.vcJsonDecoder, "vc")
  }
}
