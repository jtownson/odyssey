package net.jtownson.odyssey.impl

import io.circe
import io.circe.Json
import io.circe.syntax._
import net.jtownson.odyssey.VerificationError.ParseError
import net.jtownson.odyssey.{Jws, Signer, VP, Verifier}

import scala.concurrent.{ExecutionContext, Future}

// Encode and decode verifiable presentations as application/vp+json+jwt
object VPJwsCodec {
  import CodecStuff._

  def toJws(signer: Signer, vp: VP) = {
    Jws()
      .withHeaders(jsonHeaders(vp))
      .withSigner(signer)
  }

  def fromJwsCompactSer(verifier: Verifier, jwsSer: String)(implicit ec: ExecutionContext): Future[VP] = {
    Jws.fromCompactSer(jwsSer, verifier).flatMap { jws =>
      jws.protectedHeaders
        .get("vp")
        .fold(Future.failed[VP](ParseError("Missing vp header in JWS")))(vpJson =>
          toFuture(VPJsonCodec.vpJsonDecoder(vpJson.hcursor))
        )
    }
  }

  private def jsonHeaders(vp: VP): Map[String, Json] = {
    import VPJsonCodec._
    Seq(
      Some("cty" -> "application/vp+json".asJson),
      vp.id.map(id => "jti" -> id.asJson),
      vp.holder.map(h => "iss" -> h.asJson),
      Some("vp" -> vpJsonEncoder(vp))
    ).flatten.toMap
  }

  private def toFuture(e: Either[circe.Error, VP]): Future[VP] = {
    e.left.map(circeError => ParseError(circeError.getMessage)).fold(Future.failed, Future.successful)
  }
}