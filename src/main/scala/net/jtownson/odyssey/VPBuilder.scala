package net.jtownson.odyssey

import java.net.{URI, URL}
import java.security.PrivateKey

import net.jtownson.odyssey.VCBuilder.VCField
import net.jtownson.odyssey.VPBuilder.VPField
import net.jtownson.odyssey.VPBuilder.VPField.{EmptyField, MandatoryFields, SignatureField}
import org.jose4j.jws.AlgorithmIdentifiers

case class VPBuilder[F <: VPField, G <: VCField] private[odyssey] (
    id: Option[String] = None,
    holder: Option[URI] = None,
    additionalTypes: Seq[String] = Seq(),
    additionalContexts: Seq[URI] = Seq(),
    privateKey: Option[PrivateKey] = None,
    publicKeyRef: Option[URL] = None,
    signatureAlgo: Option[String] = None,
    vc: Option[VCBuilder[G]] = None
) {

  def withAdditionalContext(ctx: URI): VPBuilder[F, G] = {
    copy(additionalContexts = additionalContexts :+ ctx)
  }

  def withId(id: String): VPBuilder[F, G] = {
    copy(id = Some(id))
  }

  def withAdditionalType(tpe: String): VPBuilder[F, G] = {
    copy(additionalTypes = additionalTypes :+ tpe)
  }

  def withHolder(holder: URI): VPBuilder[F, G] = {
    copy(holder = Some(holder))
  }

  def withEcdsaSecp256k1Signature2019(
      publicKeyRef: URL,
      privateKey: PrivateKey
  ): VPBuilder[F with SignatureField, G] = {
    copy(
      privateKey = Some(privateKey),
      publicKeyRef = Some(publicKeyRef),
      signatureAlgo = Some(AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256)
    )
  }

  // TODO multiple VCs will need HLists I suppose - joy
  def withVC[H <: VCField](vc: VCBuilder[H]): VPBuilder[F, H] = {
    copy(vc = Some(vc))
  }

  def dataModel(implicit ev1: F =:= MandatoryFields, ev2: G =:= VCField.MandatoryFields): VP = {
    val vcs = vc.map(_.dataModel).toSeq
    VP(additionalContexts, id, additionalTypes, vcs, holder)
  }

  def toJws(implicit ev1: F =:= MandatoryFields, ev2: G =:= VCField.MandatoryFields): String = {
    VPJwsCodec.encodeJws(privateKey.get, publicKeyRef.get, signatureAlgo.get, dataModel)
  }
}

object VPBuilder {
  def apply(): VPBuilder[EmptyField, VCBuilder.VCField.EmptyField] =
    VPBuilder[EmptyField, VCBuilder.VCField.EmptyField]()

  sealed trait VPField

  object VPField {
    sealed trait EmptyField extends VPField
    sealed trait SignatureField extends VPField
    sealed trait IssuanceDateField extends VPField

    type MandatoryFields = EmptyField with SignatureField
  }
}
