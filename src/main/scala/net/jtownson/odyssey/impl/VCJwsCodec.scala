package net.jtownson.odyssey.impl

import java.time.ZoneOffset

import io.circe
import io.circe.Json
import io.circe.syntax._
import net.jtownson.odyssey.VerificationError.ParseError
import net.jtownson.odyssey.{Jws, Signer, VCDataModel, Verifier}

import scala.concurrent.{ExecutionContext, Future}

// Encode and decode verifiable credentials as application/vc+json+jwt
object VCJwsCodec {

  def toJws(signer: Signer, vc: VCDataModel) = {
    Jws()
      .withHeaders(jsonHeaders(vc))
      .withSigner(signer)
  }

  def fromJwsCompactSer(verifier: Verifier, jwsCompactSer: String)(
      implicit
      ec: ExecutionContext): Future[VCDataModel] = {
    Jws.fromCompactSer(jwsCompactSer, verifier).flatMap { jws =>
      jws.protectedHeaders
        .get("vc")
        .fold(Future.failed[VCDataModel](ParseError("Missing vc header in JWS")))(vcJson =>
          toFuture(VCJsonCodec.vcJsonDecoder(vcJson.hcursor)))
    }
  }

  private def jsonHeaders(vc: VCDataModel): Map[String, Json] = {
    import VCJsonCodec._
    Seq(
      Some("cty" -> "application/vc+json".asJson),
      vc.id.map(id => "jti" -> id.asJson),
      Some("iss" -> vc.issuer.asJson),
      Some("nbf" -> vc.issuanceDate.toEpochSecond(ZoneOffset.UTC).asJson),
      vc.expirationDate.map(exp => "exp" -> exp.toEpochSecond(ZoneOffset.UTC).asJson),
      Some("vc" -> vcJsonEncoder(vc))
    ).flatten.toMap
  }

  private def toFuture(e: Either[circe.Error, VCDataModel]): Future[VCDataModel] = {
    e.left.map(circeError => ParseError(circeError.getMessage)).fold(Future.failed, Future.successful)
  }
}
