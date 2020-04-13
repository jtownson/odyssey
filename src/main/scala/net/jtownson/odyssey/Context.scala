package net.jtownson.odyssey

import java.util.regex.Pattern

import io.circe.Json
import io.circe.parser.parse
import net.jtownson.odyssey.Context.{TermDefinition, curiePattern}

/*
From https://www.w3.org/TR/json-ld11/#context-definitions:

A context definition defines a local context in a node object.

A context definition MUST be a map whose keys MUST be either terms, compact IRIs, IRIs, or one of the keywords @base, @import, @language, @propagate, @protected, @type, @version, or @vocab.

If the context definition has an @base key, its value MUST be an IRI reference, or null.

If the context definition has an @direction key, its value MUST be one of "ltr" or "rtl", or be null.

If the context definition contains the @import keyword, its value MUST be an IRI reference. When used as a reference from an @import, the referenced context definition MUST NOT include an @import key, itself.

If the context definition has an @language key, its value MUST have the lexical form described in [BCP47] or be null.

If the context definition has an @propagate key, its value MUST be true or false.

If the context definition has an @protected key, its value MUST be true or false.

If the context definition has an @type key, its value MUST be a map with only the entry @container set to @set, and optionally an entry @protected.

If the context definition has an @version key, its value MUST be a number with the value 1.1.

If the context definition has an @vocab key, its value MUST be an IRI reference, a compact IRI, a blank node identifier, a term, or null.

The value of keys that are not keywords MUST be either an IRI, a compact IRI, a term, a blank node identifier, a keyword, null, or an expanded term definition.
 */
class Context private[odyssey] (val json: Json) {

  def pushContext(ctx: Context): Context = {
    new Context(Json.arr(ctx.json, this.json))
  }

  def expand(relativeUri: String): String = {
    JsonUtil
      .findKey("@base", json)
      .flatMap { json =>
        if (json.isString) {
          json.as[String].map(base => base + relativeUri).toOption
        } else {
          None
        }
      }
      .getOrElse(relativeUri)
  }

  def resolve(term: String): Option[String] = {
    resolveTerm(term).map(_.expand)
  }

  def resolveTerm(term: String): Option[TermDefinition] = {
    baseResolve(term, json).orElse(withVocabResolution(term))
  }

  private def baseResolve(term: String): Option[TermDefinition] = {
    baseResolve(term, json)
  }

  private def withVocabResolution(term: String): Option[TermDefinition] = {
    JsonUtil.findKeyAs[String]("@vocab", json).flatMap { vocabValue =>
      if (JsonUtil.findKey(term, json).exists(_.isNull))
        None
      else
        Some(TermDefinition(term, id = vocabValue + term))
    }
  }

  private def baseResolve(term: String, json: Json): Option[TermDefinition] = {

    def asCurie(s: String): Option[(String, String)] = {
      val m = curiePattern.matcher(s)
      if (m.find()) {
        Some((m.group(1), m.group(2)))
      } else {
        None
      }
    }

    def withCurieResolution(termDefinition: TermDefinition): TermDefinition = {
      asCurie(termDefinition.id)
        .flatMap { case (prefix, suffix) => baseResolve(prefix).map(td => td.copy(term = suffix, id = td.id + term)) }
        .getOrElse(termDefinition)
    }

    JsonUtil.findKey(term, json).flatMap { json =>
      json.fold(
        jsonNull = None,
        jsonBoolean = _ => None,
        jsonNumber = _ => None,
        jsonString = s => Some(withCurieResolution(TermDefinition(term, s))),
        jsonArray = arr =>
          arr.foldLeft(Option.empty[TermDefinition]) { (maybeTermDefinition, nextElem) =>
            if (maybeTermDefinition.isDefined)
              maybeTermDefinition
            else
              baseResolve(term, nextElem)
          },
        jsonObject = jsonObj => {
          val m = jsonObj.toMap
          m.get("@id").flatMap {
            idJson =>
              for {
                idTerm: TermDefinition <- idJson.as[String].map(id => Some(TermDefinition(term, id))).getOrElse(None)
              } yield {
                val tpeTerm: Option[String] = m.get("@type").flatMap(json => json.as[Option[String]].getOrElse(None))
                val langTerm: Option[String] = m.get("@lang").flatMap(json => json.as[Option[String]].getOrElse(None))

                val expandedTerm = idTerm.copy(tpe = tpeTerm, lang = langTerm)

                withCurieResolution(expandedTerm)
              }
          }
        }
      )
    }
  }
}

object Context {

  private val curiePattern = Pattern.compile("([\\w\\d\\-_]+):([\\w\\d\\-_]+)") // TODO Not 100% correct

  def isKeyword(str: String): Boolean =
    jsonLdKeyWords.contains(str)

  case class TermDefinition(term: String, id: String, tpe: Option[String] = None, lang: Option[String] = None) {
    def isKeywordAlias: Boolean = {
      isKeyword(id)
    }

    def expand: String = {
      if (isKeywordAlias) {
        id
      } else {
        id //+ term
      }
    }
  }

  val jsonLdKeyWords = Set(
    "@base",
    "@container",
    "@context",
    "@direction",
    "@graph",
    "@id",
    "@import",
    "@included",
    "@index",
    "@json",
    "@language",
    "@list",
    "@nest",
    "@none",
    "@prefix",
    "@propagate",
    "@protected",
    "@reverse",
    "@set",
    "@type",
    "@value",
    "@version",
    "@vocab"
  )

  def fromJson(json: Json): Context = {
    val ctx = JsonUtil
      .findKey("@context", json)
      .getOrElse(
        throw new IllegalArgumentException(
          s"Missing @context key in object used to establish JsonLd context."
        )
      )
    new Context(ctx)
  }

  def fromString(contextString: String): Context = {
    val json = parse(contextString)
      .getOrElse(
        throw new IllegalArgumentException(s"Invalid context value used to establish JsonLd context: $contextString")
      )
    fromJson(json)
  }
}
