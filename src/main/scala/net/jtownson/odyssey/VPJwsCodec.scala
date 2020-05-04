package net.jtownson.odyssey

import java.net.URL
import java.security.{PrivateKey, PublicKey}

import io.circe
import io.circe.syntax._
import io.circe.{Json, Printer, parser}
import net.jtownson.odyssey.VPJsonCodec.vpJsonEncoder
import net.jtownson.odyssey.VerificationError.{InvalidSignature, ParseError}
import org.jose4j.jwa.AlgorithmConstraints
import org.jose4j.jwa.AlgorithmConstraints.ConstraintType
import org.jose4j.jws.JsonWebSignature

import scala.concurrent.{ExecutionContext, Future}

// Encode and decode verifiable presentations as application/vp+json+jwt
object VPJwsCodec {
  import CodecStuff._
  def encodeJws(privateKey: PrivateKey, publicKeyRef: URL, signatureAlgo: String, vp: VP): String = {
    val jws: JsonWebSignature = new JsonWebSignature()
    jws.setKey(privateKey)
    jws.getHeaders.setFullHeaderAsJsonString(headers(publicKeyRef, signatureAlgo, vp))
    jws.getCompactSerialization
  }

  def decodeJws(algoWhitelist: Seq[String], publicKeyResolver: PublicKeyResolver, jwsSer: String)(implicit
      ec: ExecutionContext
  ): Future[VP] = {
    val jws = new JsonWebSignature()
    jws.setCompactSerialization(jwsSer)
    jws.setAlgorithmConstraints(new AlgorithmConstraints(ConstraintType.WHITELIST, algoWhitelist: _*))
    val publicKeyRef = new URL(jws.getHeader("kid"))

    for {
      publicKey <- publicKeyResolver.resolvePublicKey(publicKeyRef)
      _ <- verifySignature(jws, publicKey)
      vc <- toFuture(parseVp(jws))
    } yield {
      vc
    }
  }

  private def headers(publicKeyRef: URL, signatureAlgo: String, vp: VP): String = {
    Json
      .obj(
        "cty" -> "application/vp+json".asJson,
        "jti" -> vp.id.map(id => id.asJson).getOrElse(Json.Null),
        "kid" -> publicKeyRef.asJson,
        "alg" -> signatureAlgo.asJson,
        "iss" -> vp.holder.map(h => h.asJson).getOrElse(Json.Null),
        "vp" -> vpJsonEncoder(vp)
      )
      .dropNullValues
      .printWith(Printer.spaces2)
  }

  private def verifySignature(jws: JsonWebSignature, publicKey: PublicKey): Future[Unit] = {
    jws.setKey(publicKey)
    if (jws.verifySignature())
      Future.unit
    else
      Future.failed(InvalidSignature())
  }

  private def toFuture(e: Either[circe.Error, VP]): Future[VP] = {
    e.left.map(circeError => ParseError(circeError.getMessage)).fold(Future.failed, Future.successful)
  }

  private def parseVp(jws: JsonWebSignature): Either[circe.Error, VP] = {
    for {
      headerJson <- parser.parse(jws.getHeaders.getFullHeaderAsJsonString)
      hc = headerJson.hcursor
      vpJson <- hc.downField("vp").as[Json]
      vp <- VPJsonCodec.vpJsonDecoder(vpJson.hcursor)
    } yield vp
  }
}
