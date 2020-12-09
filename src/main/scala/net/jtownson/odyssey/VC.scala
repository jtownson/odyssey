package net.jtownson.odyssey

import java.net.URI
import java.time.LocalDateTime

import io.circe.syntax._
import io.circe.{Json, JsonObject, Printer}
import net.jtownson.odyssey.VC.VCField
import net.jtownson.odyssey.VC.VCField._
import net.jtownson.odyssey.impl.CodecStuff.uriEncoder
import net.jtownson.odyssey.proof.JwsSigner

import scala.concurrent.{ExecutionContext, Future}

case class VC[F <: VCField] private[odyssey] (
    additionalTypes: Seq[String] = Seq(),
    additionalContexts: Seq[Json] = Seq(),
    id: Option[String] = None,
    issuer: Option[URI] = None,
    issuerAttributes: Option[JsonObject] = None,
    subjects: Seq[JsonObject] = Seq.empty,
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

  private def buildIssuer: Json =
    issuerAttributes match {
      case Some(attributes) =>
        attributes.+:("id", issuer.get.toString.asJson).asJson
      case None =>
        issuer.get.toString.asJson
    }

  def dataModel(implicit ev: F =:= MandatoryFields): VCDataModel =
    VCDataModel(id, buildIssuer, issuanceDate.get, expirationDate, additionalTypes, additionalContexts, subjects)

  def toJws(signer: JwsSigner, printer: Printer)(implicit
      ev: F =:= MandatoryFields,
      ec: ExecutionContext
  ): Future[Jws] = {
    dataModel.toJws(signer, printer)
  }
}

object VC {

  def apply(): VC[EmptyField] = VC[EmptyField]()

  sealed trait VCField

  object VCField {
    sealed trait EmptyField extends VCField
    sealed trait CredentialSubjectField extends VCField
    sealed trait IssuerField extends VCField
    sealed trait IssuanceDateField extends VCField

    type MandatoryFields = EmptyField with IssuerField with IssuanceDateField with CredentialSubjectField
  }
}
