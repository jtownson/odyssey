package net.jtownson.odyssey

import java.net.URI

import io.circe.syntax.EncoderOps
import io.circe.{Json, Printer}
import net.jtownson.odyssey.VC.VCField
import net.jtownson.odyssey.VP.VPField
import net.jtownson.odyssey.VP.VPField.{EmptyField, MandatoryFields}
import net.jtownson.odyssey.impl.CodecStuff.uriEncoder
import net.jtownson.odyssey.proof.JwsSigner

import scala.concurrent.{ExecutionContext, Future}

case class VP[F <: VPField, G <: VCField] private[odyssey] (
    id: Option[String] = None,
    holder: Option[URI] = None,
    additionalTypes: Seq[String] = Seq(),
    additionalContexts: Seq[Json] = Seq(),
    vc: Option[VC[G]] = None
) {

  def withAdditionalContext(ctx: URI): VP[F, G] = {
    copy(additionalContexts = additionalContexts :+ ctx.asJson(uriEncoder))
  }

  def withId(id: String): VP[F, G] = {
    copy(id = Some(id))
  }

  def withAdditionalType(tpe: String): VP[F, G] = {
    copy(additionalTypes = additionalTypes :+ tpe)
  }

  def withHolder(holder: URI): VP[F, G] = {
    copy(holder = Some(holder))
  }

  // TODO multiple VCs will need HLists I suppose - joy
  def withVC[H <: VCField](vc: VC[H]): VP[F, H] = {
    copy(vc = Some(vc))
  }

  def dataModel(implicit ev1: F =:= MandatoryFields, ev2: G =:= VCField.MandatoryFields): VPDataModel = {
    val vcs = vc.map(_.dataModel).toSeq
    VPDataModel(additionalContexts, id, additionalTypes, vcs, holder)
  }

  def toJws(
      signer: JwsSigner,
      printer: Printer
  )(implicit ev1: F =:= MandatoryFields, ev2: G =:= VCField.MandatoryFields, ec: ExecutionContext): Future[Jws] = {
    dataModel.toJws(signer, printer)
  }
}

object VP {
  def apply(): VP[EmptyField, VC.VCField.EmptyField] =
    VP[EmptyField, VC.VCField.EmptyField]()

  sealed trait VPField

  object VPField {
    sealed trait EmptyField extends VPField
    sealed trait IssuanceDateField extends VPField

    type MandatoryFields = EmptyField
  }
}
