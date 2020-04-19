package net.jtownson.odyssey.ld

import java.util.regex.Pattern

import io.circe.parser.parse
import io.circe.{Json, JsonObject, Printer}
import net.jtownson.odyssey.ld.JsonLd.expandJson

class JsonLd private[odyssey] (val json: Json) {

  private val activeContext = new Context(JsonUtil.findKey("@context", json).getOrElse(Json.Null))

  def expand: JsonLd = {
    val expandedJson = expandJson(activeContext, "@default", json)
    if (expandedJson.isArray) {
      new JsonLd(expandedJson)
    } else {
      new JsonLd(Json.arr(expandedJson))
    }
  }

  override def toString(): String = {
    json.printWith(Printer.spaces2)
  }
}

object JsonLd {

//  private def expandIRI(activeContext: Context, value: Json): Option[String] = {
//    value.fold(
//      None,
//      _ => None,
//      _ => None,
//      s => expandIRI(activeContext, s),
//      _ => None
//    )
//    value match {
//      case Json.fromString(s) => expandIRI(activeContext, s)
//      case _ => None
//    }
//  }

  private def expandIRI(activeContext: Context, value: String): Option[String] = {
    def hasKeywordForm(s: String): Boolean = {
      Pattern.compile("@[\\w\\W]+").matcher(s).matches()
    }

    if (Context.isKeyword(value)) {
      Some(value)
    } else if (hasKeywordForm(value)) {
      println("DANGER DANGER DANGER")
      None
    } else {
      activeContext.resolve(value) //.orElse(Some(value))
    }
  }

  private def expandValue(activeContext: Context, activeProperty: String, value: Json): Json = {
    val maybeTerm = activeContext.resolveTerm(activeProperty)
    val hasIdType = maybeTerm.exists(td => td.tpe.contains("@id"))
    val hasVocabType = maybeTerm.exists(td => td.tpe.contains("@vocab"))
    val hasOtherType = maybeTerm.exists(td => td.tpe.exists(tpe => !Set("@id", "@vocab", "@none").contains(tpe)))
    val isStringValue = value.isString

    if ((hasIdType || hasVocabType) && isStringValue) {
      Json.obj(
        "@id" -> expandIRI(activeContext, value.as[String].getOrElse(???)).map(Json.fromString).getOrElse(Json.Null)
      )
    } else if (hasOtherType) {
      Json.obj(
        "@value" -> value,
        "@type" -> Json.fromString(maybeTerm.get.tpe.get)
      )
    } else if (isStringValue) {
      // lang/direction todo
      Json.obj(
        "@value" -> value
//        , "@type" -> Json.fromString(maybeTerm.get.tpe.get)
      )
    } else {
      value
    }
  }

  private def expandJson(activeContext: Context, activeProperty: String, element: Json): Json = {
    element.fold(
      Json.Null,
      b => expandValue(activeContext, activeProperty, Json.fromBoolean(b)),
      n => expandValue(activeContext, activeProperty, Json.fromJsonNumber(n)),
      s => expandValue(activeContext, activeProperty, Json.fromString(s)),
      arr => expandArray(activeContext, activeProperty, arr),
      obj => expandObject(activeContext, activeProperty, obj)
    )
  }

  private def expandArray(context: Context, activeProperty: String, arr: Vector[Json]): Json = {
    Json.arr(arr.map(json => expandJson(context, activeProperty, json)): _*)
  }

  private def expandObject(context: Context, activeProperty: String, jsonObject: JsonObject): Json = {
    val asMap = jsonObject.toMap
    val maybeLocalContext = asMap.get("@context").map(json => new Context(json))
    val activeContext = maybeLocalContext.fold(context)(localContext => context.pushContext(localContext))
    val expandedMap = asMap
      .map {
        case (key, value) =>
          val maybeTerm = expandIRI(activeContext, key)
          maybeTerm match {
            case Some(term) =>
              val v = value.fold(
                Json.Null,
                b => Json.fromBoolean(b),
                n => Json.fromJsonNumber(n),
                s => expandValue(activeContext, activeProperty, Json.fromString(s)),
                arr => ???,
                jobj => expandObject(context, activeProperty, jobj)
              )
              Some(term -> v)
            case None =>
              None
          }
      }
      .filter(_.isDefined)
      .map(_.get)

    Json.fromFields(expandedMap)
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
