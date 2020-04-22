package net.jtownson.odyssey

sealed trait VerificationError extends Exception

object VerificationError {
  case class InvalidSignature() extends VerificationError
  case class ParseError() extends VerificationError
  case class PublicKeyResolutionError() extends VerificationError
}
