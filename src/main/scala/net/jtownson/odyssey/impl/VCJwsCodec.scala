package net.jtownson.odyssey.impl

import java.nio.charset.StandardCharsets
import java.nio.charset.StandardCharsets.UTF_8
import java.time.ZoneOffset

import io.circe
import io.circe.{Json, JsonObject}
import io.circe.syntax._
import net.jtownson.odyssey.VerificationError.ParseError
import net.jtownson.odyssey.impl.VCJsonCodec._
import net.jtownson.odyssey.{Jws, Signer, VCDataModel, Verifier}

import scala.concurrent.{ExecutionContext, Future}

// Encode and decode verifiable credentials as application/vc+json+jwt
object VCJwsCodec {

  def toJws(signer: Signer, vc: VCDataModel) = {
    Jws()
      .withHeaders(jsonHeaders(vc))
      .withJsonPayload(Json.obj("vc" -> vcJsonEncoder(vc)))
      .withSigner(signer)
  }

  def fromJwsCompactSer(verifier: Verifier, jwsCompactSer: String)(implicit
      ec: ExecutionContext
  ): Future[VCDataModel] = {
    Jws.fromCompactSer(jwsCompactSer, verifier).flatMap { jws =>
      io.circe.parser
        .parse(new String(jws.payload, UTF_8))
        .fold(
          failure => Future.failed[VCDataModel](failure),
          json =>
            toFuture(VCJsonCodec.vcJsonDecoder(json.hcursor.downField("vc").as[Json].getOrElse(Json.Null).hcursor))
        )
    }
  }

  private def jsonHeaders(vc: VCDataModel): Map[String, Json] = {
    Seq(
      Some("cty" -> "application/vc+json".asJson),
      vc.id.map(id => "jti" -> id.asJson),
      Some("iss" -> vc.issuer.asJson),
      Some("nbf" -> vc.issuanceDate.toEpochSecond(ZoneOffset.UTC).asJson),
      vc.expirationDate.map(exp => "exp" -> exp.toEpochSecond(ZoneOffset.UTC).asJson)
    ).flatten.toMap
  }

  private def toFuture(e: Either[circe.Error, VCDataModel]): Future[VCDataModel] = {
    e.left.map(circeError => ParseError(circeError.getMessage)).fold(Future.failed, Future.successful)
  }
}
