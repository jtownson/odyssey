package net.jtownson.odyssey

import java.net.URI
import java.security.PrivateKey
import java.time.LocalDateTime

import io.circe.syntax._
import io.circe.{Json, JsonObject}
import net.jtownson.odyssey.Signer.Es256Signer
import net.jtownson.odyssey.VC.VCField
import net.jtownson.odyssey.VC.VCField._
import net.jtownson.odyssey.impl.CodecStuff.uriEncoder
import net.jtownson.odyssey.impl.VCJwsCodec

case class VC[F <: VCField] private[odyssey] (
    additionalTypes: Seq[String] = Seq(),
    additionalContexts: Seq[Json] = Seq(),
    id: Option[String] = None,
    issuer: Option[URI] = None,
    issuerAttributes: Option[JsonObject] = None,
    subjects: Seq[JsonObject] = Seq.empty,
    signer: Option[Signer] = None,
    issuanceDate: Option[LocalDateTime] = None,
    expirationDate: Option[LocalDateTime] = None
) {
  def withId(id: String): VC[F] = {
    copy(id = Some(id))
  }

  def withIssuer(issuer: URI): VC[F with IssuerField] = {
    copy(issuer = Some(issuer))
  }

  def withIssuerAttributes(issuerAttributes: (String, Json)*): VC[F] = {
    copy(issuerAttributes = Some(JsonObject(issuerAttributes: _*)))
  }

  def withIssuanceDate(iss: LocalDateTime): VC[F with IssuanceDateField] = {
    copy(issuanceDate = Some(iss))
  }

  def withExpirationDate(exp: LocalDateTime): VC[F] = {
    copy(expirationDate = Some(exp))
  }

  def withSubjectAttributes(subjectAttributes: (String, Json)*): VC[F with CredentialSubjectField] = {
    copy(subjects = Seq(JsonObject(subjectAttributes: _*)))
  }

  def withAdditionalType(tpe: String): VC[F] = {
    copy(additionalTypes = additionalTypes :+ tpe)
  }

  def withAdditionalContext(ctx: URI): VC[F] = {
    copy(additionalContexts = additionalContexts :+ ctx.asJson(uriEncoder))
  }

  def withSigner(signer: Signer): VC[F with SignatureField] = {
    copy(signer = Some(signer))
  }

  def withEs256Signature(
      publicKeyRef: URI,
      privateKey: PrivateKey
  ): VC[F with SignatureField] = {
    withSigner(new Es256Signer(publicKeyRef, privateKey))
  }

  private def buildIssuer: Json =
    issuerAttributes match {
      case Some(attributes) =>
        attributes.+:("id", issuer.get.toString.asJson).asJson
      case None =>
        issuer.get.toString.asJson
    }

  def dataModel(implicit ev: F =:= MandatoryFields): VCDataModel =
    VCDataModel(id, buildIssuer, issuanceDate.get, expirationDate, additionalTypes, additionalContexts, subjects)

  def toJws(implicit ev: F =:= MandatoryFields) = {
    val vc =
      VCDataModel(id, buildIssuer, issuanceDate.get, expirationDate, additionalTypes, additionalContexts, subjects)
    VCJwsCodec.toJws(signer.get, vc)
  }
}

object VC {

  def apply(): VC[EmptyField] = VC[EmptyField]()

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
