package net.jtownson.odyssey

import java.net.{URI, URL}
import java.security.PrivateKey

import net.jtownson.odyssey.Signer.Es256Signer
import net.jtownson.odyssey.VCBuilder.VCField
import net.jtownson.odyssey.VPBuilder.VPField
import net.jtownson.odyssey.VPBuilder.VPField.{EmptyField, MandatoryFields, SignatureField}
import net.jtownson.odyssey.impl.VPJwsCodec

case class VPBuilder[F <: VPField, G <: VCField] private[odyssey] (
    id: Option[String] = None,
    holder: Option[URI] = None,
    additionalTypes: Seq[String] = Seq(),
    additionalContexts: Seq[URI] = Seq(),
    signer: Option[Signer] = None,
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

  def withEs256Signature(
      publicKeyRef: URL,
      privateKey: PrivateKey
  ): VPBuilder[F with SignatureField, G] = {
    withSigner(new Es256Signer(publicKeyRef, privateKey))
  }

  def withSigner(signer: Signer): VPBuilder[F with SignatureField, G] = {
    copy(signer = Some(signer))
  }

  // TODO multiple VCs will need HLists I suppose - joy
  def withVC[H <: VCField](vc: VCBuilder[H]): VPBuilder[F, H] = {
    copy(vc = Some(vc))
  }

  def dataModel(implicit ev1: F =:= MandatoryFields, ev2: G =:= VCField.MandatoryFields): VP = {
    val vcs = vc.map(_.dataModel).toSeq
    VP(additionalContexts, id, additionalTypes, vcs, holder)
  }

  def toJws(implicit ev1: F =:= MandatoryFields, ev2: G =:= VCField.MandatoryFields) = {
    VPJwsCodec.toJws(signer.get, dataModel)
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
