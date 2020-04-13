package net.jtownson.odyssey

import java.net.URL
import java.util.regex.Pattern

import io.circe.{Json, JsonObject}
import io.circe.parser.parse
import net.jtownson.odyssey.JsonLd.{expandArray, invalid}

class JsonLd private[odyssey] (val json: Json) {

  def expand: JsonLd = {
    json.fold(
      this,
      jsonBoolean => invalid(jsonBoolean),
      jsonNumber => invalid(jsonNumber),
      jsonString => invalid(jsonString),
      jsonArray => new JsonLd(expandArray(jsonArray)),
      jsonObject => new JsonLd(JsonLd.expandObject(jsonObject))
    )
  }

  override def toString(): String = {
    ???
  }
}

object JsonLd {

  private def expandIRI(activeContext: Context, value: Json): Json = {

    def hasKeywordForm(s: String): Boolean = {
      Pattern.compile("@[\\w\\W]+").matcher(s).matches()
    }

    value.fold(
      Json.Null,
      jsonBoolean => ???,
      jsonNumber => ???,
      jsonString => {
        if (Context.isKeyword(jsonString)) {
          Json.fromString(jsonString)
        } else if (hasKeywordForm(jsonString)) {
          println("DANGER DANGER DANGER")
          Json.Null
        } else {
          activeContext.resolve(jsonString).orElse(Some(jsonString)).map(Json.fromString).get
        }
      },
      jsonArray => ???,
      jsonObject => ???
    )
  }

  private def expandValue(activeContext: Context, activeProperty: String, value: Json): Json = {
    val maybeTerm = activeContext.resolveTerm(activeProperty)
    val hasIdType = maybeTerm.exists(td => td.tpe.contains("@id"))
    val hasVocabType = maybeTerm.exists(td => td.tpe.contains("@vocab"))
    val hasOtherType = maybeTerm.exists(td => td.tpe.exists(tpe => !Set("@id", "@vocab", "@none").contains(tpe)))
    val isStringValue = value.isString

    if ((hasIdType || hasVocabType) && isStringValue) {
      Json.obj(
        "@id" -> expandIRI(activeContext, value)
      )
    } else if (hasOtherType) {
      Json.obj(
        "@value" -> value,
        "@type" -> Json.fromString(maybeTerm.get.tpe.get)
      )
    } else if (isStringValue) {
      // lang/direction todo
      Json.obj(
        "@value" -> value,
        "@type" -> Json.fromString(maybeTerm.get.tpe.get)
      )
    } else {
      value
    }
  }

  private def expandJson(activeContext: Context, activeProperty: String, element: Json, baseUrl: URL): Json = {
    ???
//    activeContext.resolve(activeProperty)
  }

  private def expandArray(v: Vector[Json]): Json = {
    ???
//    Json.arr(v.map(json => JsonLd.expandJson(json)): _*)
  }

  private def expandObject(jsonObject: JsonObject): Json = {
    ???
  }

  private def expandObject(activeContext: Context, activeProperty: String, jsonObject: JsonObject): Json = {
    val asMap = jsonObject.toMap
    val maybeLocalContext = asMap.get("@context").map(json => new Context(json))

//    asMap.map { case (key, value) =>
//      val term = expandIRI()
//    }
    ???
  }

  def fromJson(json: Json): JsonLd = {
    if (json.isObject || json.isArray)
      new JsonLd(json)
    else
      invalid(json)
  }

  def fromString(str: String): JsonLd = {
    fromJson(
      parse(str)
        .getOrElse(throw new IllegalArgumentException(s"Invalid JSON used to establish JsonLd."))
    )
  }

  private def invalid[T](t: T): JsonLd = {
    throw new IllegalArgumentException(s"Invalid JSON-LD. Object or array expected. Got $t.")
  }
}
