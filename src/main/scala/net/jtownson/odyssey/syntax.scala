package net.jtownson.odyssey

import java.net._

import io.circe.Json
import io.circe.syntax._

object syntax {
  implicit def str2Json(s: String): Json = {
    s.asJson
  }
  implicit def str2URI(s: String): URI = {
    new URI(s)
  }
  implicit def str2URL(s: String): URL = {
    new URL(s)
  }
}
