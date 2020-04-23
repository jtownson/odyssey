package net.jtownson.odyssey

import java.net.{URI, URL}
import java.security.PrivateKey
import java.time.LocalDateTime

import io.circe.{Json, JsonObject}
import net.jtownson.odyssey.VCBuilder.MandatoryField
import net.jtownson.odyssey.VCBuilder.MandatoryField._
import net.jtownson.odyssey.syntax._
import org.jose4j.jws.AlgorithmIdentifiers

case class VCBuilder[F <: MandatoryField] private[odyssey] (
    types: Seq[String] = Seq("VerifiableCredential"),
    contexts: Seq[URI] = Seq("https://www.w3.org/2018/credentials/v1"),
    id: Option[String] = None,
    issuer: Option[URI] = None,
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

  def withIssuanceDate(iss: LocalDateTime): VCBuilder[F] = {
    copy(issuanceDate = Some(iss))
  }

  def withExpirationDate(exp: LocalDateTime): VCBuilder[F] = {
    copy(expirationDate = Some(exp))
  }

  def withCredentialSubject(claims: (String, Json)*): VCBuilder[F with CredentialSubjectField] = {
    copy(subjects = Seq(JsonObject(claims: _*)))
  }

  def withAdditionalType(tpe: String): VCBuilder[F] = {
    copy(types = types :+ tpe)
  }

  def withAdditionalContext(ctx: URI): VCBuilder[F] = {
    copy(contexts = contexts :+ ctx)
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

  def toJws: String = {
    val vc = VC(id, issuer.get, issuanceDate.get, expirationDate, types, contexts, subjects)
    JwsCodec.encodeJws(privateKey.get, publicKeyRef.get, signatureAlgo.get, vc)
  }
}

object VCBuilder {

  def apply(): VCBuilder[EmptyField] = VCBuilder[EmptyField]()

  sealed trait MandatoryField

  object MandatoryField {
    sealed trait EmptyField extends MandatoryField
    sealed trait CredentialSubjectField extends MandatoryField
    sealed trait SignatureField extends MandatoryField
    sealed trait IssuerField extends MandatoryField
    sealed trait IssuanceDateField extends MandatoryField

    type MandatoryFields = EmptyField
      with IssuerField
      with IssuanceDateField
      with CredentialSubjectField
      with SignatureField
  }
}
