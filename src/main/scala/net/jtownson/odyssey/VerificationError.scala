package net.jtownson.odyssey

import io.circe.Json

sealed trait VerificationError extends Exception

object VerificationError {
  case class InvalidSignature() extends VerificationError
  case class ParseError() extends VerificationError
  case class PublicKeyResolutionError() extends VerificationError
  case class VCDataModelError(msg: String) extends VerificationError

  val ContextShouldBeAnArrayOrString = VCDataModelError(
    "@context must the string 'https://www.w3.org/2018/credentials/v1' " +
      "or a multi-element array starting with this element. " +
      "(see vctest suite 'MUST be one or more URIs')"
  )
}
