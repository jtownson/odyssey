package net.jtownson.odyssey

import java.net.{URI, URL}
import java.security.PrivateKey
import java.time.LocalDateTime

import io.circe.{Json, JsonObject}
import net.jtownson.odyssey.VC.{ParsedVc, SerializedJwsVC}
import net.jtownson.odyssey.VCBuilder.LinkedDatasetField
import net.jtownson.odyssey.VCBuilder.LinkedDatasetField._
import org.jose4j.jws.AlgorithmIdentifiers

case class VCBuilder[F <: LinkedDatasetField] private[odyssey] (
    id: Option[String] = None,
    issuer: Option[URI] = None,
    subjects: Seq[JsonObject] = Seq.empty,
    privateKey: Option[PrivateKey] = None,
    publicKeyRef: Option[URL] = None,
    signatureAlgo: Option[String] = None,
    issuanceDate: Option[LocalDateTime] = None,
    expirationDate: Option[LocalDateTime] = None,
) {
  def withId(id: String): VCBuilder[F with IdField] = {
    copy(id = Some(id))
  }

  def withIssuer(issuer: URI): VCBuilder[F with IssuerField] = {
    copy(issuer = Some(issuer))
  }

  def withIssuanceDate(iss: LocalDateTime): VCBuilder[F with IssuanceDateField] = {
    copy(issuanceDate = Some(iss))
  }

  def withExpirationDate(exp: LocalDateTime): VCBuilder[F with ExpirationDateField] = {
    copy(expirationDate = Some(exp))
  }

  def withCredentialSubject(claims: (String, Json)*): VCBuilder[F with ClaimsField] = {
    copy(subjects = Seq(JsonObject(claims: _*)))
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

  def toJws: String = build.jws

  private def build: SerializedJwsVC = {
    val vc = ParsedVc(id, issuer.get, issuanceDate, expirationDate, subjects)
    val jws = JwsCodec.encodeJws(privateKey.get, publicKeyRef.get, signatureAlgo.get, vc)
    SerializedJwsVC(id, issuer.get, issuanceDate, expirationDate, subjects, jws)
  }
}

object VCBuilder {

  def apply(): VCBuilder[EmptyField] = VCBuilder[EmptyField]()

  sealed trait LinkedDatasetField

  object LinkedDatasetField {
    sealed trait IdField extends LinkedDatasetField
    sealed trait EmptyField extends LinkedDatasetField
    sealed trait ContextField extends LinkedDatasetField
    sealed trait SubjectField extends LinkedDatasetField
    sealed trait ExpirationDateField extends LinkedDatasetField
    sealed trait ClaimsField extends LinkedDatasetField
    sealed trait SignatureField extends LinkedDatasetField
    sealed trait ContentField extends LinkedDatasetField
    sealed trait IssuerField extends LinkedDatasetField
    sealed trait IssuanceDateField extends LinkedDatasetField

    type MandatoryFields = EmptyField with IssuerField with IssuanceDateField with SubjectField with SignatureField
  }
}
