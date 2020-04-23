package net.jtownson.odyssey

import java.net.URI
import java.time.LocalDateTime

import io.circe.JsonObject

case class VP(
    contexts: Seq[URI],
    id: Option[String],
    types: Seq[String],
    verifiableCredential: Seq[VC],
    holder: Option[URI]
)

object VP {
  def fromJsonLd(jsonLdSer: String): Either[VerificationError, VP] =
    VPJsonCodec.decodeJsonLd(jsonLdSer)
}
