package net.jtownson.odyssey

import java.net.{URI, URL}
import java.security.PrivateKey
import java.time.LocalDateTime

import io.circe.{Json, JsonObject}
import io.circe.syntax._
import net.jtownson.odyssey.VCBuilder.VCField
import net.jtownson.odyssey.VCBuilder.VCField._
import org.jose4j.jws.AlgorithmIdentifiers

case class VCBuilder[F <: VCField] private[odyssey] (
    additionalTypes: Seq[String] = Seq(),
    additionalContexts: Seq[URI] = Seq(),
    id: Option[String] = None,
    issuer: Option[URI] = None,
    issuerAttributes: Option[JsonObject] = None,
    subjects: Seq[JsonObject] = Seq.empty,
    privateKey: Option[PrivateKey] = None,
    publicKeyRef: Option[URL] = None,
    signatureAlgo: Option[String] = None,
    issuanceDate: Option[LocalDateTime] = None,
    expirationDate: Option[LocalDateTime] = None
) {
  def withId(id: String): VCBuilder[F] = {
    copy(id = Some(id))
  }

  def withIssuer(issuer: URI): VCBuilder[F with IssuerField] = {
    copy(issuer = Some(issuer))
  }

  def withIssuerAttributes(issuerAttributes: (String, Json)*): VCBuilder[F] = {
    copy(issuerAttributes = Some(JsonObject(issuerAttributes: _*)))
  }

  def withIssuanceDate(iss: LocalDateTime): VCBuilder[F with IssuanceDateField] = {
    copy(issuanceDate = Some(iss))
  }

  def withExpirationDate(exp: LocalDateTime): VCBuilder[F] = {
    copy(expirationDate = Some(exp))
  }

  def withSubjectAttributes(subjectAttributes: (String, Json)*): VCBuilder[F with CredentialSubjectField] = {
    copy(subjects = Seq(JsonObject(subjectAttributes: _*)))
  }

  def withAdditionalType(tpe: String): VCBuilder[F] = {
    copy(additionalTypes = additionalTypes :+ tpe)
  }

  def withAdditionalContext(ctx: URI): VCBuilder[F] = {
    copy(additionalContexts = additionalContexts :+ ctx)
  }

  def withEcdsaSecp256k1Signature2019(
      publicKeyRef: URL,
      privateKey: PrivateKey
  ): VCBuilder[F with SignatureField] = {
    copy(
      privateKey = Some(privateKey),
      publicKeyRef = Some(publicKeyRef),
      signatureAlgo = Some(AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256)
    )
  }

  private def buildIssuer: Json =
    issuerAttributes match {
      case Some(attributes) =>
        attributes.+:("id", issuer.get.toString.asJson).asJson
      case None =>
        issuer.get.toString.asJson
    }

  def dataModel(implicit ev: F =:= MandatoryFields): VC =
    VC(id, buildIssuer, issuanceDate.get, expirationDate, additionalTypes, additionalContexts, subjects)

  // TODO introduce JWS abstraction and emit that instead of string
  def toJws(implicit ev: F =:= MandatoryFields): String = {
    val vc = VC(id, buildIssuer, issuanceDate.get, expirationDate, additionalTypes, additionalContexts, subjects)
    VCJwsCodec.encodeJws(privateKey.get, publicKeyRef.get, signatureAlgo.get, vc)
  }
}

object VCBuilder {

  def apply(): VCBuilder[EmptyField] = VCBuilder[EmptyField]()

  sealed trait VCField

  object VCField {
    sealed trait EmptyField extends VCField
    sealed trait CredentialSubjectField extends VCField
    sealed trait SignatureField extends VCField
    sealed trait IssuerField extends VCField
    sealed trait IssuanceDateField extends VCField

    type MandatoryFields = EmptyField
      with IssuerField
      with IssuanceDateField
      with CredentialSubjectField
      with SignatureField
  }
}
