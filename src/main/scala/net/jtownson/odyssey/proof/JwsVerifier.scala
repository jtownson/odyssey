package net.jtownson.odyssey.proof

import java.nio.charset.StandardCharsets.UTF_8

import io.circe.parser.parse
import io.circe.{Decoder, Json}
import net.jtownson.odyssey.{Jws, VerificationError}
import net.jtownson.odyssey.VerificationError.{InvalidSignature, ParseError}
import net.jtownson.odyssey.proof.JwsVerifier.VerificationResult

import scala.concurrent.{ExecutionContext, Future}

trait JwsVerifier {
  def verify(jws: Jws): Future[VerificationResult]
}

object JwsVerifier {
  case class VerificationResult(headersValid: Boolean, signatureValid: Boolean) {
    val valid = headersValid && signatureValid
  }

  def fromJws[T](verifier: JwsVerifier, jwsCompactSer: String, jsonCodec: Decoder[T], bodyElement: String)(implicit
      ec: ExecutionContext
  ): Future[T] = {
    val parseResult: Either[VerificationError, Future[T]] = for {
      jws <- Jws.fromCompactSer(jwsCompactSer)
      payloadJson <- parse(new String(jws.payload, UTF_8)).left.map(_ =>
        ParseError(s"Unable to parse body to JSON: '$jwsCompactSer'.")
      )
      bodyJson <-
        payloadJson.hcursor
          .downField(bodyElement)
          .as[Json]
          .left
          .map(_ => ParseError(s"No '$bodyElement' entry in body JSON: '$jwsCompactSer'."))
      vc <- jsonCodec(bodyJson.hcursor).left.map(_ =>
        ParseError(s"$bodyElement entry in body cannot be read: '$jwsCompactSer'.")
      )
    } yield {
      for {
        verificationResult <- verifier.verify(jws)
        resultFuture <- resultToFuture(vc, verificationResult)
      } yield resultFuture
    }
    parseResult.fold(Future.failed(_), identity)
  }

  private def resultToFuture[T](vc: T, verificationResult: VerificationResult) = {
    if (verificationResult.valid) {
      Future.successful(vc)
    } else {
      Future.failed(InvalidSignature())
    }
  }
}
