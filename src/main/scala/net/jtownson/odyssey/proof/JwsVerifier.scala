package net.jtownson.odyssey.proof

import java.nio.charset.StandardCharsets.UTF_8

import io.circe.parser.parse
import io.circe.{Decoder, Json}
import net.jtownson.odyssey.VerificationError.{InvalidSignature, ParseError}
import net.jtownson.odyssey.proof.JwsVerifier.VerificationResult
import net.jtownson.odyssey.{Jws, VerificationError}

import scala.concurrent.{ExecutionContext, Future}

trait JwsVerifier {
  def verify(jws: Jws): Future[VerificationResult]
}

object JwsVerifier {
  case class VerificationResult(headersValid: Boolean, signatureValid: Boolean) {
    val valid = headersValid && signatureValid
  }

  def fromJws[T](
      verifier: JwsVerifier,
      jwsCompactSer: String,
      jsonCodec: Decoder[T],
      fixupBodyElement: Json => Either[ParseError, Json]
  )(implicit
      ec: ExecutionContext
  ): Future[T] = {
    val parseResult = parseJws(jwsCompactSer, jsonCodec, fixupBodyElement)
    parseResult.fold(
      parserError => Future.failed(parserError),
      result => {
        val (jws, t) = result
        verifier.verify(jws).flatMap(vr => resultToFuture[T](t, vr))
      }
    )
  }

  def fromJws[T](
      jwsCompactSer: String,
      jsonCodec: Decoder[T],
      fixupBodyElement: Json => Either[ParseError, Json]
  ): Either[VerificationError, T] = {
    parseJws(jwsCompactSer, jsonCodec, fixupBodyElement).map(_._2)
  }

  private def parseJws[T](
      jwsCompactSer: String,
      jsonCodec: Decoder[T],
      fixupBodyElement: Json => Either[ParseError, Json] = Right(_)
  ): Either[VerificationError, (Jws, T)] = {
    for {
      jws <- Jws.fromCompactSer(jwsCompactSer)
      payloadJson <- parseBody(jwsCompactSer, jws)
      payloadJsonFixup <- fixupBodyElement(payloadJson)
      t <- parseT(jwsCompactSer, jsonCodec, payloadJsonFixup)
    } yield (jws, t)
  }

  private def parseT[T](
      jwsCompactSer: String,
      jsonCodec: Decoder[T],
      claimElement: Json
  ): Either[ParseError, T] = {
    jsonCodec(claimElement.hcursor).left.map(decodingFailure =>
      ParseError(
        s"Claim entry in body cannot be read due to decoding failure '${decodingFailure.message}'. Jws: '$jwsCompactSer'."
      )
    )
  }

  private def parseBody[T](jwsCompactSer: String, jws: Jws): Either[ParseError, Json] = {
    parse(new String(jws.payload, UTF_8)).left.map(_ => ParseError(s"Unable to parse body to JSON: '$jwsCompactSer'."))
  }

  private def resultToFuture[T](vc: T, verificationResult: VerificationResult) = {
    if (verificationResult.valid) {
      Future.successful(vc)
    } else {
      Future.failed(InvalidSignature())
    }
  }
}
