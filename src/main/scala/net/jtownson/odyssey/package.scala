package net.jtownson

import java.io.{StringReader, StringWriter}
import java.net.{URI, URL, URLConnection, URLStreamHandler, URLStreamHandlerFactory}
import java.security.PublicKey
import java.util

import net.jtownson.odyssey.RDFNode.{Literal, ResourceNode}
import net.jtownson.odyssey.RDFNode.ResourceNode.{BNode, URINode}
import net.jtownson.odyssey.VerificationError.InvalidSignature
import org.apache.jena.rdf.model
import org.apache.jena.rdf.model.{Model, ModelFactory, Property, Resource, ResourceFactory, Statement, StmtIterator}
import org.jose4j.jwa.AlgorithmConstraints
import org.jose4j.jwa.AlgorithmConstraints.ConstraintType
import org.jose4j.jws.{AlgorithmIdentifiers, JsonWebSignature}

import scala.util.Try

//import com.github.jsonldjava.core.{JsonLdApi, JsonLdOptions, RDFDataset, RDFDatasetUtils}
//import com.github.jsonldjava.utils.JsonUtils
import net.jtownson.odyssey.LinkedDatasetBuilder.LinkedDatasetField
import net.jtownson.odyssey.LinkedDatasetBuilder.LinkedDatasetField.{
  ClaimsField,
  ContextField,
  EmptyField,
  MandatoryFields,
  SignatureField,
  TypeField
}
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
    implicit def uri2ResourceNode(uri: URI): ResourceNode = {
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

    sealed trait ResourceNode extends RDFNode

    object ResourceNode {

      case class URINode(value: URI) extends ResourceNode

      case class BNode private[odyssey] (label: String) extends ResourceNode

      def fold[T](fUri: URI => T, fBNode: String => T)(node: ResourceNode): T =
        node match {
          case URINode(value) =>
            fUri(value)
          case BNode(label) =>
            fBNode(label)
        }
    }

    case class Literal(
        lexicalForm: String,
        datatype: URI = Literal.stringType,
        langOpt: Option[String] = None
    ) extends RDFNode

    object Literal {
      val stringType = new URI("http://www.w3.org/1999/02/22-rdf-syntax-ns#langString")
    }

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

  case class LinkedDatasetBuilder[F <: LinkedDatasetField] private[odyssey] (
      context: Option[Context] = None,
      tpe: Option[String] = None,
      claims: Seq[(ResourceNode, URI, RDFNode)] = Seq.empty,
      privateKey: Option[PrivateKey] = None,
      publicKeyRef: Option[URL] = None,
      signatureAlgo: Option[String] = None
  ) {
    def withContext(context: Context): LinkedDatasetBuilder[F with ContextField] = {
      copy(context = Some(context))
    }

    def withType(term: String): LinkedDatasetBuilder[F with TypeField] = {
      copy(tpe = Some(term))
    }

    def withStatements(values: (ResourceNode, URI, RDFNode)*): LinkedDatasetBuilder[F with ClaimsField] = {
      copy(claims = values)
    }

    def withEcdsaSecp256k1Signature2019(
        publicKeyRef: URL,
        privateKey: PrivateKey
    ): LinkedDatasetBuilder[F with SignatureField] = {
      copy(
        privateKey = Some(privateKey),
        publicKeyRef = Some(publicKeyRef),
        signatureAlgo = Some(AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256)
      )
    }

    def toJWS(implicit ev: F <:< MandatoryFields): String = {
      def createSignature(payload: String): Option[String] = {
        for {
          key <- privateKey
          algo <- signatureAlgo
        } yield {
          val jws: JsonWebSignature = new JsonWebSignature()
          jws.setPayload(payload)
          jws.setAlgorithmHeaderValue(algo)
          jws.setKey(key)
          jws.getCompactSerialization
        }
      }
      val sw = new StringWriter()
      build.rdfModel.write(sw, "JSON-LD")
      val jsonLdSer = sw.toString

      createSignature(jsonLdSer).getOrElse(
        throw new IllegalStateException("SignatureField manadatory. This should not compile.")
      )
    }

    def toProto(implicit ev: F <:< MandatoryFields): Array[Byte] = ??? // TODO?

    private def build: LinkedDataset = {

      def toJenaResource(n: ResourceNode, model: Model): Resource = {
        ResourceNode.fold(uri => model.createResource(uri.toString), _ => ???)(n)
      }

      def toJenaNode(n: RDFNode, model: Model): org.apache.jena.rdf.model.RDFNode = {
        RDFNode.fold(
          uri => model.createResource(uri.toString),
          _ => ???,
          (lex, dataType, maybeLang) => {
            maybeLang.fold(createTypedLiteral(lex, dataType, model))(lang => model.createLiteral(lex, lang))
          }
        )(n)
      }

      def createTypedLiteral(lex: String, dataType: URI, model: Model) = {
        if (dataType == Literal.stringType)
          model.createLiteral(lex)
        else
          model.createTypedLiteral(lex, dataType.toString)
      }

      import scala.jdk.CollectionConverters._
      val rdfModel = context.fold(ModelFactory.createDefaultModel())(
        ctx => ModelFactory.createDefaultModel().setNsPrefixes(ctx.nsMap.asJava)
      )

      val claimModel = claims.foldLeft(rdfModel) { (accModel, next) =>
        val (subj, pred, obj) = next
        val statement = accModel.createStatement(
          toJenaResource(subj, accModel),
          accModel.createProperty(pred.toString),
          toJenaNode(obj, accModel)
        )
        accModel.add(statement)
      }

      new LinkedDataset(claimModel)
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

      type MandatoryFields = EmptyField with SignatureField
    }
  }

  case class LinkedDataset private[odyssey] (rdfModel: Model)

  object LinkedDataset {
    def withContext(context: Context) = LinkedDatasetBuilder().withContext(context)
    def withType(term: String) = LinkedDatasetBuilder().withType(term)
    def withClaims(values: (ResourceNode, URI, RDFNode)*) = LinkedDatasetBuilder().withStatements(values: _*)

    def fromJws(jwsSer: String, publicKey: PublicKey): Either[VerificationError, LinkedDataset] = {
      val jws = new JsonWebSignature()
      jws.setCompactSerialization(jwsSer)
      jws.setKey(publicKey)
      jws.setAlgorithmConstraints(
        new AlgorithmConstraints(ConstraintType.WHITELIST, AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256)
      )
      if (jws.verifySignature()) {
        val payload = jws.getPayload

        val rdfModel = ModelFactory.createDefaultModel().read(new StringReader(payload), "", "JSON-LD")

        Right(LinkedDataset(rdfModel))
      } else {
        Left(InvalidSignature)
      }
    }
  }

  trait VerificationError

  object VerificationError {
    case object InvalidSignature extends VerificationError
  }
}
