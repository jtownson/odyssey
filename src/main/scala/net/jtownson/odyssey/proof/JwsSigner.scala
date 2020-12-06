package net.jtownson.odyssey.proof

import java.time.ZoneOffset

import io.circe.{Json, Printer}
import net.jtownson.odyssey.Jws.utf8
import net.jtownson.odyssey.impl.CodecStuff
import net.jtownson.odyssey.impl.VCJsonCodec.vcJsonEncoder
import net.jtownson.odyssey.impl.VPJsonCodec.vpJsonEncoder
import net.jtownson.odyssey.{Jws, VCDataModel, VPDataModel}

import scala.concurrent.{ExecutionContext, Future}

trait JwsSigner {

  def sign(data: Array[Byte]): Future[Array[Byte]]

  def getAlgorithmHeaders: Map[String, Json]
}

object JwsSigner {

  import CodecStuff._
  import io.circe.syntax.EncoderOps

  def sign(vp: VPDataModel, printer: Printer, signer: JwsSigner)(implicit ec: ExecutionContext): Future[Jws] = {
    val protectedHeaders = getPresentationHeaders(vp)
    sign(protectedHeaders, Json.obj("vp" -> vpJsonEncoder(vp)), printer, signer)
  }

  def sign(vc: VCDataModel, printer: Printer, signer: JwsSigner)(implicit ec: ExecutionContext): Future[Jws] = {
    val protectedHeaders = getCredentialHeaders(vc)
    sign(protectedHeaders, Json.obj("vc" -> vcJsonEncoder(vc)), printer, signer)
  }

  def sign(protectedHeaders: Map[String, Json], payloadJson: Json, printer: Printer, signer: JwsSigner)(implicit
      ec: ExecutionContext
  ): Future[Jws] = {
    sign(protectedHeaders, utf8(printer.print(payloadJson)), printer, signer)
  }

  def sign(protectedHeaders: Map[String, Json], payload: Array[Byte], printer: Printer, signer: JwsSigner)(implicit
      ec: ExecutionContext
  ): Future[Jws] = {
    val allProtectedHeaders = protectedHeaders ++ signer.getAlgorithmHeaders
    val headers = printer.print(allProtectedHeaders.asJson)
    val utf8Headers = utf8(headers)
    signer
      .sign(Jws.signingInput(utf8Headers, payload))
      .map(signature => Jws(protectedHeaders, utf8Headers, payload, signature))
  }

  private def getCredentialHeaders(vc: VCDataModel): Map[String, Json] = {
    Seq(
      Some("cty" -> "application/vc+json".asJson),
      vc.id.map(id => "jti" -> id.asJson),
      Some("iss" -> vc.issuer.asJson),
      Some("nbf" -> vc.issuanceDate.toEpochSecond(ZoneOffset.UTC).asJson),
      vc.expirationDate.map(exp => "exp" -> exp.toEpochSecond(ZoneOffset.UTC).asJson)
    ).flatten.toMap
  }

  private def getPresentationHeaders(vp: VPDataModel): Map[String, Json] = {
    Seq(
      Some("cty" -> "application/vp+json".asJson),
      vp.id.map(id => "jti" -> id.asJson),
      vp.holder.map(h => "iss" -> h.asJson)
    ).flatten.toMap
  }
}
