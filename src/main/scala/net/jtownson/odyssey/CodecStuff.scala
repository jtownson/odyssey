package net.jtownson.odyssey

import java.net.{URI, URL}
import java.time.{LocalDateTime, ZoneId}
import java.time.format.DateTimeFormatter

import io.circe.{Decoder, Encoder}

object CodecStuff {
  implicit val urlEncoder: Encoder[URL] = Encoder[String].contramap(_.toString)
  implicit val urlDecoder: Decoder[URL] = Decoder[String].map(new URL(_))

  implicit val uriEncoder: Encoder[URI] = Encoder[String].contramap(_.toString)
  implicit val uriDecoder: Decoder[URI] = Decoder[String].map(new URI(_))

  implicit val localDateTimeEncoder: Encoder[LocalDateTime] =
    Encoder[String].contramap(d => dfRfc3339.format(d))

  implicit val localDateTimeDecoder: Decoder[LocalDateTime] =
    Decoder[String].map(dateStr => LocalDateTime.from(dfRfc3339.parse(dateStr)))

  val absoluteUriDecoder: Decoder[URI] =
    uriDecoder.ensure(uri => uri.isAbsolute, "Require an absolute URI at this position.")

  private val dfRfc3339 = DateTimeFormatter
    .ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
    .withZone(ZoneId.of("UTC"))

}
