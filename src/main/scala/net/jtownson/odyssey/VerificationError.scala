package net.jtownson.odyssey

trait VerificationError

object VerificationError {
  case object InvalidSignature extends VerificationError
}
