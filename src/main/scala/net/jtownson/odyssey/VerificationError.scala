package net.jtownson.odyssey

sealed abstract class VerificationError(val msg: String) extends Exception(msg)

object VerificationError {
  case class InvalidSignature() extends VerificationError("Invalid signature detected")
  case class ParseError(override val msg: String) extends VerificationError(msg)
  case class PublicKeyResolutionError() extends VerificationError("Failed to resolve public verification key")
  case class VCDataModelError(override val msg: String) extends VerificationError(msg)

  val ContextShouldBeAnArrayOrString = VCDataModelError(
    "@context must the string 'https://www.w3.org/2018/credentials/v1' " +
      "or a multi-element array starting with this element. " +
      "(see vctest suite 'MUST be one or more URIs')"
  )
}
