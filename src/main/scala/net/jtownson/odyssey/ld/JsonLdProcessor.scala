package net.jtownson.odyssey.ld

import net.jtownson.odyssey.ld.JsonLdProcessor._

import scala.concurrent.Future
import scala.io.Source
import scala.util.{Failure, Success}

trait JsonLdProcessor {

  def parse(input: Source): Future[JsonLd]

  def compact(
      input: JsonLd,
      context: Option[JsonLd],
      options: Option[JsonLdOptions]
  ): Future[JsonLd]

  def expand(
      input: JsonLd,
      options: Option[JsonLdOptions]
  ): Future[Seq[JsonLd]]

  def flatten(
      input: JsonLd,
      context: Option[JsonLd],
      options: Option[JsonLdOptions]
  ): Future[JsonLd]

  def fromRdf(
      input: RdfDataset,
      options: Option[JsonLdOptions]
  ): Future[Seq[JsonLd]]

  def toRdf(
      input: JsonLd,
      options: Option[JsonLdOptions]
  ): Future[RdfDataset]
}

object JsonLdProcessor {
  type RdfDataset = Map[String, RdfGraph]
  type RdfGraph = Set[RdfTriple]

  case class RdfTriple(subj: String, pred: String, obj: RdfObject)

  sealed trait RdfObject
  object RdfObject {
    case class ObjectString(value: String) extends RdfObject
    case class ObjectRdfLiteral(value: RdfLiteral) extends RdfObject
  }
  case class RdfLiteral(value: String, datatype: String, language: Option[String])

  sealed trait JsonLd
  object JsonLd {
    case class JsonLdObject(value: Map[String, JsonLd]) extends JsonLd
    case class JsonLdSeq(values: Seq[JsonLd]) extends JsonLd
    case class JsonLdString(value: String) extends JsonLd

    object JsonLdObject {
      def apply(values: (String, JsonLd)*): JsonLdObject =
        JsonLdObject(Map(values: _*))

      def fold[T](fObj: Map[String, JsonLd] => T, fSeq: Seq[JsonLd] => T, fStr: String => T)(jsonLd: JsonLd): T =
        jsonLd match {
          case JsonLdObject(value) =>
            fObj(value)
          case JsonLdSeq(values) =>
            fSeq(values)
          case JsonLdString(value) =>
            fStr(value)
        }
    }
  }

  case class JsonLdOptions(
      base: Option[String] = None,
      compactArrays: Boolean = true,
      compactToRelative: Boolean = true,
      documentLoader: Option[LoadDocumentCallback] = None,
      expandContext: Option[JsonLd] = None,
      extractAllScripts: Boolean = false,
      frameExpansion: Boolean = false,
      ordered: Boolean = false,
      processingMode: String = "json-ld-1.1",
      produceGeneralizedRdf: Boolean = true,
      rdfDirection: Option[String] = None,
      useNativeTypes: Boolean = false,
      useRdfType: Boolean = false
  )

  type LoadDocumentCallback =
    (String, Option[LoadDocumentOptions]) => Future[RemoteDocument]

  case class LoadDocumentOptions(
      extractAllScripts: Boolean = false,
      profile: Option[String] = None,
      requestProfile: Seq[String] = Seq.empty
  )

  case class RemoteDocument(
      contextUrl: Option[String] = None,
      documentUrl: String,
      document: Source,
      contentType: String,
      profile: Option[String] = None
  )

  case class JsonLdError(code: JsonLdErrorCode, message: Option[String] = None) extends RuntimeException

  sealed trait JsonLdErrorCode

  object JsonLdErrorCode {
    case object `IRI confused with prefix` extends JsonLdErrorCode
    case object `colliding keywords` extends JsonLdErrorCode
    case object `conflicting indexes` extends JsonLdErrorCode
    case object `context overflow` extends JsonLdErrorCode
    case object `cyclic IRI mapping` extends JsonLdErrorCode
    case object `invalid @id value` extends JsonLdErrorCode
    case object `invalid @import value` extends JsonLdErrorCode
    case object `invalid @included value` extends JsonLdErrorCode
    case object `invalid @index value` extends JsonLdErrorCode
    case object `invalid @nest value` extends JsonLdErrorCode
    case object `invalid @prefix value` extends JsonLdErrorCode
    case object `invalid @propagate value` extends JsonLdErrorCode
    case object `invalid @protected value` extends JsonLdErrorCode
    case object `invalid @reverse value` extends JsonLdErrorCode
    case object `invalid @version value` extends JsonLdErrorCode
    case object `invalid IRI mapping` extends JsonLdErrorCode
    case object `invalid JSON literal` extends JsonLdErrorCode
    case object `invalid base IRI` extends JsonLdErrorCode
    case object `invalid base direction` extends JsonLdErrorCode
    case object `invalid container mapping` extends JsonLdErrorCode
    case object `invalid context entry` extends JsonLdErrorCode
    case object `invalid context nullification` extends JsonLdErrorCode
    case object `invalid default language` extends JsonLdErrorCode
    case object `invalid keyword alias` extends JsonLdErrorCode
    case object `invalid language map value` extends JsonLdErrorCode
    case object `invalid language mapping` extends JsonLdErrorCode
    case object `invalid language-tagged string` extends JsonLdErrorCode
    case object `invalid language-tagged value` extends JsonLdErrorCode
    case object `invalid local context` extends JsonLdErrorCode
    case object `invalid remote context` extends JsonLdErrorCode
    case object `invalid reverse property map` extends JsonLdErrorCode
    case object `invalid reverse property value` extends JsonLdErrorCode
    case object `invalid reverse property` extends JsonLdErrorCode
    case object `invalid scoped context` extends JsonLdErrorCode
    case object `invalid script element` extends JsonLdErrorCode
    case object `invalid set or list object` extends JsonLdErrorCode
    case object `invalid term definition` extends JsonLdErrorCode
    case object `invalid type mapping` extends JsonLdErrorCode
    case object `invalid type value` extends JsonLdErrorCode
    case object `invalid typed value` extends JsonLdErrorCode
    case object `invalid value object value` extends JsonLdErrorCode
    case object `invalid value object` extends JsonLdErrorCode
    case object `invalid vocab mapping` extends JsonLdErrorCode
    case object `keyword redefinition` extends JsonLdErrorCode
    case object `loading document failed` extends JsonLdErrorCode
    case object `loading remote context failed` extends JsonLdErrorCode
    case object `multiple context link headers` extends JsonLdErrorCode
    case object `processing mode conflict` extends JsonLdErrorCode
    case object `protected term redefinition` extends JsonLdErrorCode
  }
}
