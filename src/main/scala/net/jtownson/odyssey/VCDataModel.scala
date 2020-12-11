package net.jtownson.odyssey

import java.net.URI
import java.net.URI.create
import java.time.{LocalDateTime, ZoneOffset}

import io.circe.Json.JArray
import io.circe.syntax.EncoderOps
import io.circe.{DecodingFailure, Json, JsonObject, Printer}
import net.jtownson.odyssey.VerificationError.ParseError
import net.jtownson.odyssey.impl.{CodecStuff, VCJsonCodec}
import net.jtownson.odyssey.impl.VCJsonCodec.{vcJsonDecoder, vcJsonEncoder}
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
    Seq(Some("cty" -> "application/vc+json".asJson)).flatten.toMap
  }

  private def credentialClaims(vcJson: Json): Map[String, Json] = {
    Seq(
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
    JwsVerifier.fromJws[VCDataModel](verifier, jwsCompactSer, vcJsonDecoder, "vc")
  }

  def fromJws(jwsCompactSer: String): Either[VerificationError, VCDataModel] = {
    JwsVerifier.fromJws[VCDataModel](jwsCompactSer, vcJsonDecoder, "vc", json => credentialFixup(json))
  }

  private def credentialFixup(payloadJson: Json): Either[ParseError, Json] = {
    /*
    If exp is present, the UNIX timestamp MUST be converted to an [RFC3339] date-time,
    and MUST be used to set the value of the expirationDate property of credentialSubject of the new JSON object.

    If iss is present, the value MUST be used to set the issuer property of the new JSON object.

    If nbf is present, the UNIX timestamp MUST be converted to an [RFC3339] date-time,
    and MUST be used to set the value of the issuanceDate property of the new JSON object.'

    If sub is present, the value MUST be used to set the value of the id property of credentialSubject of the new JSON object.

    If jti is present, the value MUST be used to set the value of the id property of the new JSON object.
     */
    import CodecStuff._
    def fixExp(vc: Map[String, Json], maybeExp: Option[Long]): Map[String, Json] = {
      fixUnixTimestamp("expirationDate", vc, maybeExp)
    }

    def fixIss(vc: Map[String, Json], maybeIss: Option[URI]): Map[String, Json] = {
      maybeIss.fold(vc) { iss => vc + ("issuer" -> iss.asJson) }
    }

    def fixNbf(vc: Map[String, Json], maybeNbf: Option[Long]): Map[String, Json] = {
      fixUnixTimestamp("issuanceDate", vc, maybeNbf)
    }

    def fixSub(vc: Map[String, Json], maybeSub: Option[URI]): Map[String, Json] = {
      maybeSub.fold(vc) { sub => vc + ("credentialSubject" -> Json.obj("id" -> sub.asJson)) }
    }

    def fixJti(vc: Map[String, Json], maybeJti: Option[URI]): Map[String, Json] = {
      maybeJti.fold(vc) { jti => vc + ("id" -> jti.asJson) }
    }

    def fixUnixTimestamp(elmt: String, vc: Map[String, Json], maybeTs: Option[Long]): Map[String, Json] = {
      maybeTs.fold(vc) { ts =>
        val expDateTime = LocalDateTime.ofEpochSecond(ts, 0, ZoneOffset.UTC)
        vc + (elmt -> expDateTime.asJson)
      }
    }

    val hc = payloadJson.hcursor
    val f: Either[DecodingFailure, Json] = for {
      vc <- hc.downField("vc").as[Map[String, Json]]
      exp <- hc.downField("exp").as[Option[Long]]
      iss <- hc.downField("iss").as[Option[URI]]
      nbf <- hc.downField("nbf").as[Option[Long]]
      sub <- hc.downField("sub").as[Option[URI]]
      jti <- hc.downField("jti").as[Option[URI]]
    } yield {
      fixExp(fixIss(fixNbf(fixSub(fixJti(vc, jti), sub), nbf), iss), exp).asJson
    }
    f.left.map(decodingFailure => ParseError(decodingFailure.message))
  }
}
