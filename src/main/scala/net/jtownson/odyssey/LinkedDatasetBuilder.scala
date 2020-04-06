package net.jtownson.odyssey

import java.io.StringWriter
import java.net.{URI, URL}
import java.security.PrivateKey
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import net.jtownson.odyssey.LinkedDatasetBuilder.LinkedDatasetField
import net.jtownson.odyssey.LinkedDatasetBuilder.LinkedDatasetField._
import net.jtownson.odyssey.RDFNode.{Literal, ResourceNode}
import org.apache.jena.rdf.model.{Model, ModelFactory, Resource}
import org.jose4j.jws.{AlgorithmIdentifiers, JsonWebSignature}

case class LinkedDatasetBuilder[F <: LinkedDatasetField] private[odyssey] (
    nsMap: Map[String, String] = Map.empty,
    tpe: Option[String] = None,
    claims: Seq[(ResourceNode, URI, RDFNode)] = Seq.empty,
    privateKey: Option[PrivateKey] = None,
    publicKeyRef: Option[URL] = None,
    signatureAlgo: Option[String] = None,
    issuanceDate: Option[LocalDate] = None,
    expiryDate: Option[LocalDate] = None
) {
  def withIssuanceDate(iss: LocalDate): LinkedDatasetBuilder[F with IssuanceDateField] = {
    copy(issuanceDate = Some(iss))
  }

  def withExpiryDate(exp: LocalDate): LinkedDatasetBuilder[F with ExpiryDateField] = {
    copy(expiryDate = Some(exp))
  }

  def withNamespaces(nsMappings: (String, String)*): LinkedDatasetBuilder[F with ContextField] = {
    copy(nsMap = nsMappings.toMap)
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
    val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    def createSignature(payload: String): Option[String] = {
      for {
        key <- privateKey
        kid <- publicKeyRef
        algo <- signatureAlgo
      } yield {
        val jws: JsonWebSignature = new JsonWebSignature()
        jws.setPayload(payload)
        jws.setAlgorithmHeaderValue(algo)
        jws.setKey(key)
        jws.setHeader("kid", kid.toString)

        issuanceDate.foreach(iss => jws.setHeader("iss", dateFormatter.format(iss)))
        expiryDate.foreach(exp => jws.setHeader("exp", dateFormatter.format(exp)))

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
    val rdfModel = ModelFactory.createDefaultModel().setNsPrefixes(nsMap.asJava)

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
    sealed trait ClaimsField extends LinkedDatasetField
    sealed trait SignatureField extends LinkedDatasetField

    sealed trait ExpirationDateField extends LinkedDatasetField
    sealed trait IssuerField extends LinkedDatasetField
    sealed trait IssuanceDateField extends LinkedDatasetField
    sealed trait ExpiryDateField extends LinkedDatasetField

    type MandatoryFields = EmptyField with SignatureField with IssuanceDateField
  }
}
