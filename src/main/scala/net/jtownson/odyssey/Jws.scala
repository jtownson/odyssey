package net.jtownson.odyssey

import java.nio.charset.StandardCharsets.UTF_8
import java.util.Base64

import io.circe.syntax._
import io.circe.{DecodingFailure, Json, JsonObject, ParsingFailure, Printer}
import net.jtownson.odyssey.Jws.JwsField.{EmptyField, JsonPayloadField, SignatureField}
import net.jtownson.odyssey.Jws.{JwsField, MandatoryFields, utf8}
import net.jtownson.odyssey.VerificationError.InvalidSignature
import org.jose4j.lang.StringUtil

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

// Providing JWS rather than use an existing java library
// because we want to guarantee compatibility with scalaJs
// and require support for custom proof algorithms.
case class Jws[F <: JwsField] private[odyssey] (
    printer: Printer = Printer.spaces2,
    protectedHeaders: Map[String, Json] = Map.empty,
    payload: Array[Byte] = Array.empty,
    signer: Option[Signer] = None,
    signature: Option[Array[Byte]] = None
) {

  def withJsonPrinter(printer: Printer): Jws[F] = {
    copy(printer = printer)
  }

  def withHeaders(protectedHeaders: Map[String, Json]): Jws[F] = {
    copy(protectedHeaders = protectedHeaders)
  }

  def withHeader(name: String, value: Json): Jws[F] = {
    copy(protectedHeaders = protectedHeaders + (name -> value))
  }

  def withHeader(name: String, value: String): Jws[F] = {
    copy(protectedHeaders = protectedHeaders + (name -> value.asJson))
  }

  def withJsonPayload(payload: Json): Jws[F] = {
    copy(payload = payload.printWith(printer).getBytes(UTF_8))
  }

  def withRawPayload(payload: Array[Byte]): Jws[F] = {
    copy(payload = payload)
  }

  def withRawPayload(contentType: String, payload: Array[Byte]): Jws[F] = {
    withHeader("cty", contentType).withRawPayload(payload)
  }

  def withStringPayload(payload: String): Jws[F] = {
    withRawPayload(payload.getBytes(UTF_8))
  }

  def withStringPayload(contentType: String, payload: String): Jws[F] = {
    withHeader("cty", contentType).withStringPayload(payload)
  }

  def withSigner(signer: Signer): Jws[F with SignatureField] = {
    signer.setHeaderParameters(this).copy(signer = Some(signer))
  }

  private def withSignature(signature: Array[Byte]): Jws[F with SignatureField] = {
    copy(signature = Some(signature))
  }

  def compactSerializion(implicit ev: F =:= MandatoryFields): String = {
    if (signature.isDefined) {
      Jws.compactSerialization(utf8ProtectedHeader, payload, signature.get)
    } else {
      Jws.compactSerialization(utf8ProtectedHeader, payload, signer.get)
    }
  }

  def utf8ProtectedHeader: Array[Byte] = {
    utf8(protectedHeaders.asJson.printWith(printer))
  }
}

object Jws {

  def apply(): Jws[EmptyField] = new Jws[EmptyField]()

  def fromCompactSerialization(ser: String, verifier: Verifier)(implicit ec: ExecutionContext) = {
    parse(ser) match {
      case Success(ParseResult(protectedHeaders, signerInput, payload, signature)) =>
        verifier
          .verify(protectedHeaders, signerInput, signature)
          .flatMap { success: Boolean =>
            if (success) {
              Future.successful(
                Jws()
                  .withHeaders(protectedHeaders)
                  .withRawPayload(payload)
                  .withSignature(signature)
              )
            } else {
              Future.failed(InvalidSignature())
            }
          }
      case Failure(t) =>
        Future.failed(t)
    }
  }

  sealed trait JwsField

  object JwsField {
    sealed trait EmptyField extends JwsField
    sealed trait JsonPayloadField extends JwsField
    sealed trait SignatureField extends JwsField
  }

  type MandatoryFields = EmptyField with SignatureField

  def base64Url(data: Array[Byte]): String = {
    Base64.getUrlEncoder.withoutPadding().encodeToString(data)
  }

  def base64Url(dataEnc: String): Array[Byte] = {
    Base64.getUrlDecoder.decode(dataEnc)
  }

  def ascii(data: String): Array[Byte] = {
    StringUtil.getBytesAscii(data)
  }

  def utf8(data: String): Array[Byte] = {
    data.getBytes(UTF_8)
  }

  def signingInput(utf8ProtectedHeader: Array[Byte], payload: Array[Byte]): Array[Byte] = {
    // ASCII(BASE64URL(UTF8(JWS Protected Header)) || '.' || BASE64URL(JWS Payload))
    ascii(s"${base64Url(utf8ProtectedHeader)}.${base64Url(payload)}")
  }

  def sign(utf8ProtectedHeader: Array[Byte], payload: Array[Byte], signer: Signer): Array[Byte] = {
    signer.sign(signingInput(utf8ProtectedHeader, payload))
  }

  def compactSerialization(
      utf8ProtectedHeader: Array[Byte],
      payload: Array[Byte],
      signingDevice: Signer
  ): String = {
    val signature = sign(utf8ProtectedHeader, payload, signingDevice)
    compactSerialization(utf8ProtectedHeader, payload, signature)
  }

  def compactSerialization(
      utf8ProtectedHeader: Array[Byte],
      payload: Array[Byte],
      signature: Array[Byte]
  ): String = {
    //    BASE64URL(UTF8(JWS Protected Header)) || '.' ||
    //    BASE64URL(JWS Payload) || '.' ||
    //    BASE64URL(JWS Signature)
    s"${base64Url(utf8ProtectedHeader)}.${base64Url(payload)}.${base64Url(signature)}"
  }

  private[Jws] case class ParseResult(
      protectedHeaders: Map[String, Json],
      signerInput: Array[Byte],
      payload: Array[Byte],
      signature: Array[Byte]
  )

  def parse(compactSer: String): Try[ParseResult] = {
    val parts = compactSer.split('.')
    if (parts.length != 3) {
      Failure(new IllegalArgumentException(s"Expect three parts in JWS compact serialization. Got ${parts.length}"))
    } else {
      val (headerEnc, payloadEnc, sigEnc) = (parts(0), parts(1), parts(2))
      val headerDec = new String(Base64.getUrlDecoder.decode(headerEnc), UTF_8)
      val headerParseResult: Either[ParsingFailure, Json] = io.circe.parser.parse(headerDec)

      val signerInput = ascii(compactSer.substring(0, compactSer.lastIndexOf('.')))

      val payloadDec = Base64.getUrlDecoder.decode(payloadEnc)
      val sigDec = Base64.getUrlDecoder.decode(sigEnc)

      val parseResult = headerParseResult.flatMap { headerJson: Json =>
        if (!headerJson.isObject) {
          Left(DecodingFailure(s"JWS header is not a valid JSON object: $headerDec", List.empty))
        } else {
          headerJson.as[JsonObject].map(headerObj => ParseResult(headerObj.toMap, signerInput, payloadDec, sigDec))
        }
      }

      parseResult.toTry
    }
  }
}
