package net.jtownson

import java.net.{URI, URL, URLConnection, URLStreamHandler, URLStreamHandlerFactory}
import java.security.PrivateKey

import net.jtownson.odyssey.LinkedDatasetBuilder.LinkedDatasetField
import net.jtownson.odyssey.LinkedDatasetBuilder.LinkedDatasetField.{ClaimsField, ContextField, EmptyField, MandatoryFields, SignatureField, TypeField}
import net.jtownson.odyssey.RDFNode.URINode

import scala.io.Source
import scala.util.{Failure, Success, Try, Using}

package object odyssey {

  import io.circe.{Json => CirceJson}

  type Json = CirceJson

  object syntax {
    implicit def did2RDFNode(did: DID): RDFNode = {
      URINode(did.value.toURI)
    }
    implicit def str2URI(s: String): URI = {
      new URI(s)
    }
    implicit def str2RDFNode(s: String): RDFNode = {
      URINode(new URI(s))
    }
  }

  case class DID private(value: URL)

  object DID {

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

  trait TermDefinition {
    def resolve(term: String): URI
  }

  object TermDefinition {
    case class URITermDefinition(value: URI) extends TermDefinition {
      override def resolve(term: String): URI = new URI(s"${value.toString.replaceFirst("/$", "")}/$term")
    }
  }

  case class Context private(termDefs: Map[String, TermDefinition]) {
    def resolve(term: String): URI = ??? // TODO
  }

  object Context {

    /**
     * Load a json-ld context from a URL with blocking.
     * @param url the URL to load.
     * @return the parsed Json-ld context.
     */
    def fromURLSync(url: URL): Context = {
      Using(Source.fromURL(url, "UTF-8"))(source => source.mkString) match {
        case Success(value) =>
          fromString(value)
        case Failure(exception) =>
          throw exception
      }
    }

    def fromURLSync(url: String): Context = {
      fromURLSync(new URL(url))
    }

    def fromString(s: String): Context = {
      Context(Map()) // TODO
    }
  }


  sealed trait RDFNode
  object RDFNode {

    case class URINode(value: URI) extends RDFNode

    case class BNode(label: String) extends RDFNode

    case class Literal(lexicalForm: String, datatype: URI, langOpt: Option[String]) extends RDFNode
  }

  case class LinkedDatasetBuilder[F <: LinkedDatasetField](context: Option[Context] = None, tpe: Option[String] = None, claims: Seq[(RDFNode, URI, RDFNode)] = Seq.empty,
                                                           privateKey: Option[PrivateKey] = None, publicKeyRef: Option[URL] = None) {
    def withContext(context: Context): LinkedDatasetBuilder[F with ContextField] = {
      copy(context = Some(context))
    }

    def withType(term: String): LinkedDatasetBuilder[F with TypeField] = {
      copy(tpe = Some(term))
    }

    def withClaims(values: (RDFNode, URI, RDFNode)*): LinkedDatasetBuilder[F with ClaimsField] = {
      copy(claims = values)
    }

    def withEd25519Signature2018(privateKey: PrivateKey, publicKeyRef: URL): LinkedDatasetBuilder[F with SignatureField] = {
      copy(privateKey = Some(privateKey), publicKeyRef = Some(publicKeyRef))
    }

    def toJson(implicit ev: F <:< MandatoryFields): Json = {
      ??? // TODO
    }

    def mkString(implicit ev: F <:< MandatoryFields): Json = {
      ??? // TODO
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

  class LinkedDataset {
  }

  object LinkedDataset {
    def withContext(context: Context) = LinkedDatasetBuilder().withContext(context)
    def withType(term: String) = LinkedDatasetBuilder().withType(term)
    def withClaims(values: (RDFNode, URI, RDFNode)*) = LinkedDatasetBuilder().withClaims(values: _*)
  }
}
