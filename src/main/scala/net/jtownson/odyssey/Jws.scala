package net.jtownson.odyssey

import java.nio.charset.StandardCharsets.{US_ASCII, UTF_8}
import java.util.Base64

import io.circe.{parser, _}
import io.circe.syntax.EncoderOps
import net.jtownson.odyssey.VerificationError.{InvalidSignature, ParseError}

// Providing JWS rather than use an existing java library
// because we require support for custom proof algorithms.
case class Jws(
    protectedHeaders: Map[String, Json],
    utf8ProtectedHeaders: Array[Byte],
    payload: Array[Byte],
    signature: Array[Byte]
) {

  def compactSerialization: String = {
    Jws.compactSerialization(utf8ProtectedHeaders, payload, signature)
  }
}

object Jws {

  def apply(
      utf8ProtectedHeaders: Array[Byte],
      payload: Array[Byte],
      signature: Array[Byte]
  ): Either[ParseError, Jws] = {
    parser
      .parse(new String(utf8ProtectedHeaders, UTF_8))
      .left
      .map(e => ParseError(e.message))
      .flatMap(headerJson =>
        headerJson
          .as[Map[String, Json]]
          .left
          .map(f => ParseError(f.message))
          .map(headers => Jws(headers, utf8ProtectedHeaders, payload, signature))
      )
  }

  def utf8ProtectedHeaders(printer: Printer, jsonHeaders: Map[String, Json]): Array[Byte] = {
    utf8(printer.print(jsonHeaders.asJson))
  }

  def fromCompactSer(ser: String): Either[ParseError, Jws] = {
    parse(ser)
  }

  def base64Url(data: Array[Byte]): String = {
    Base64.getUrlEncoder.withoutPadding().encodeToString(data)
  }

  def base64Url(dataEnc: String): Array[Byte] = {
    Base64.getUrlDecoder.decode(dataEnc)
  }

  def ascii(data: String): Array[Byte] = {
    data.getBytes(US_ASCII)
  }

  def signingInput(jws: Jws): Array[Byte] = {
    signingInput(jws.utf8ProtectedHeaders, jws.payload)
  }

  def signingInput(utf8ProtectedHeader: Array[Byte], payload: Array[Byte]): Array[Byte] = {
    // ASCII(BASE64URL(UTF8(JWS Protected Header)) || '.' || BASE64URL(JWS Payload))
    ascii(s"${base64Url(utf8ProtectedHeader)}.${base64Url(payload)}")
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

  private def parse(compactSer: String): Either[ParseError, Jws] = {
    val parts = compactSer.split('.')
    if (parts.length != 3) {
      Left(ParseError(s"Insufficient parts in JWS '$compactSer'"))
    } else {
      val (headerEnc, payloadEnc, sigEnc) = (parts(0), parts(1), parts(2))
      val headerDec = Base64.getUrlDecoder.decode(headerEnc)
      val payloadDec = Base64.getUrlDecoder.decode(payloadEnc)
      val sigDec = Base64.getUrlDecoder.decode(sigEnc)

      Jws(headerDec, payloadDec, sigDec)
    }
  }

  def utf8(data: String): Array[Byte] = {
    data.getBytes(UTF_8)
  }
}
