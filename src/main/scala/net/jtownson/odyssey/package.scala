package net.jtownson

import java.net.{URI, URL, URLConnection, URLStreamHandler, URLStreamHandlerFactory}
import java.util

import com.github.jsonldjava.core.{JsonLdApi, JsonLdOptions, RDFDataset, RDFDatasetUtils}
import com.github.jsonldjava.utils.JsonUtils
import net.jtownson.odyssey.LinkedDatasetBuilder.LinkedDatasetField
import net.jtownson.odyssey.LinkedDatasetBuilder.LinkedDatasetField.{ClaimsField, ContextField, EmptyField, MandatoryFields, SignatureField, TypeField}
import net.jtownson.odyssey.RDFNode.URINode
import net.jtownson.odyssey.ld.JsonLdProcessor

import scala.concurrent.Future
import scala.io.Source
import scala.util.{Failure, Success, Using}

package object odyssey {

  import io.circe.{Json => CirceJson}

  type Json = CirceJson
  type PrivateKey = java.security.PrivateKey

  object syntax {
    implicit def did2RDFNode(did: DID): RDFNode = {
      URINode(did.value.toURI)
    }
    implicit def url2RDFNode(url: URL): RDFNode = {
      URINode(url.toURI)
    }
    implicit def rui2RDFNode(uri: URI): RDFNode = {
      URINode(uri)
    }

    implicit def str2URI(s: String): URI = {
      new URI(s)
    }
    implicit def str2URL(s: String): URL = {
      new URL(s)
    }
  }

  case class DID private (value: URL)

  object DID {

    // define did: as a valid URL prefix and provide the JDK with
    // a way to resolve did: URLs.
    URL.setURLStreamHandlerFactory(new DIDStreamHandlerFactory())

    def apply(method: String, suffix: String): DID = {
      DID(new URL(s"did:$method:$suffix"))
    }

    class DIDConnection(url: URL) extends URLConnection(url) {
      override def connect(): Unit = {
        System.out.println("Connected!");
      }
    }

    class DIDStreamHandler extends URLStreamHandler {
      override def openConnection(url: URL): URLConnection = {
        new DIDConnection(url)
      }
    }

    class DIDStreamHandlerFactory extends URLStreamHandlerFactory {
      override def createURLStreamHandler(protocol: String): URLStreamHandler = {
        if ("did" == protocol) {
          new DIDStreamHandler()
        } else {
          null
        }
      }
    }
  }

  sealed trait TermDefinition {
    def resolve(term: String): URI
  }

  object TermDefinition {
    case class URITermDefinition(value: URI) extends TermDefinition {
      override def resolve(term: String): URI = new URI(s"${value.toString.replaceFirst("/$", "")}/$term")
    }

    def fold[T](fUri: URI => T)(termDefinition: TermDefinition): T = termDefinition match {
      case URITermDefinition(value) => fUri(value)
    }
  }

  case class Context private (nsMap: Map[String, String])

  object Context {
    def withNamespaces(nsMappings: (String, String)*): Context = {
      Context(nsMappings.toMap)
    }
  }

  sealed trait RDFNode

  object RDFNode {

    case class URINode(value: URI) extends RDFNode

    case class BNode(label: String) extends RDFNode

    case class Literal(
        lexicalForm: String,
        datatype: URI = new URI("http://www.w3.org/1999/02/22-rdf-syntax-ns#langString"),
        langOpt: Option[String] = None
    ) extends RDFNode

    def fold[T](fUri: URI => T, fBNode: String => T, fLiteral: (String, URI, Option[String]) => T)(node: RDFNode): T =
      node match {
        case URINode(value) =>
          fUri(value)
        case BNode(label) =>
          fBNode(label)
        case Literal(lexicalForm, datatype, langOpt) =>
          fLiteral(lexicalForm, datatype, langOpt)
      }
  }

  case class LinkedDatasetBuilder[F <: LinkedDatasetField](
      context: Option[Context] = None,
      tpe: Option[String] = None,
      claims: Seq[(RDFNode, URI, RDFNode)] = Seq.empty,
      privateKey: Option[PrivateKey] = None,
      publicKeyRef: Option[URL] = None
  ) {
    def withContext(context: Context): LinkedDatasetBuilder[F with ContextField] = {
      copy(context = Some(context))
    }

    def withType(term: String): LinkedDatasetBuilder[F with TypeField] = {
      copy(tpe = Some(term))
    }

    def withClaims(values: (RDFNode, URI, RDFNode)*): LinkedDatasetBuilder[F with ClaimsField] = {
      copy(claims = values)
    }

    def withEd25519Signature2018(
        privateKey: PrivateKey,
        publicKeyRef: URL
    ): LinkedDatasetBuilder[F with SignatureField] = {
      copy(privateKey = Some(privateKey), publicKeyRef = Some(publicKeyRef))
    }

    def toJson(implicit ev: F <:< MandatoryFields): Json = {
      val rdfDataset = build.rdfDataset

      val api = new JsonLdApi()
      val s = JsonUtils.toPrettyString(api.fromRDF(rdfDataset))
      val compact = com.github.jsonldjava.core.JsonLdProcessor.compact(api.fromRDF(rdfDataset),rdfDataset.getContext, new JsonLdOptions())
      println(JsonUtils.toPrettyString(compact))
      val jsonLd = api.fromRDF(rdfDataset).get(0).asInstanceOf[util.Map[String, AnyRef]]
      io.circe.parser.parse(JsonUtils.toPrettyString(jsonLd))
        .getOrElse(throw new IllegalStateException("RDF dataset not compatible with JSON encoding. Impossible?"))
    }

    def toProto(implicit ev: F <:< MandatoryFields): Array[Byte] = ??? // TODO?

    private def build: LinkedDataset = {
      val rdfDataset = new RDFDataset()

      context.foreach { ctx =>
        ctx.nsMap.foreach { case (ns, prefix) =>
          rdfDataset.setNamespace(ns, prefix)
        }
      }

      claims.foreach {
        case (subj, pred, obj) =>
          val subjStr = RDFNode.fold(uri => uri.toString, identity, (_, _, _) => ???)(subj)
          val predStr = pred.toString
          val objStr = RDFNode.fold(uri => uri.toString, identity, (lexicalForm, _, _) => lexicalForm)(obj)

          rdfDataset.addTriple(subjStr, predStr, objStr)
      }

      new LinkedDataset(rdfDataset)
    }
  }

  object LinkedDatasetBuilder {

    def apply(): LinkedDatasetBuilder[EmptyField] = LinkedDatasetBuilder[EmptyField]()

    sealed trait LinkedDatasetField

    object LinkedDatasetField {
      sealed trait EmptyField extends LinkedDatasetField
      sealed trait ContextField extends LinkedDatasetField
      sealed trait TypeField extends LinkedDatasetField
      sealed trait ClaimsField extends LinkedDatasetField
      sealed trait SignatureField extends LinkedDatasetField

      type MandatoryFields = EmptyField
    }
  }

  case class LinkedDataset private[odyssey] (rdfDataset: RDFDataset)

  object LinkedDataset {
    def withContext(context: Context) = LinkedDatasetBuilder().withContext(context)
    def withType(term: String) = LinkedDatasetBuilder().withType(term)
    def withClaims(values: (RDFNode, URI, RDFNode)*) = LinkedDatasetBuilder().withClaims(values: _*)

    def fromJson(json: Json): Future[LinkedDataset] = ??? // TODO
  }
}
