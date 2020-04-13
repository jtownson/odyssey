package net.jtownson.odyssey

import java.io.StringReader
import java.net.{URI, URL}
import java.security.PublicKey
import java.time.LocalDate

import net.jtownson.odyssey.RDFNode.ResourceNode
import net.jtownson.odyssey.VerificationError.InvalidSignature
import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.jose4j.jwa.AlgorithmConstraints
import org.jose4j.jwa.AlgorithmConstraints.ConstraintType
import org.jose4j.jws.{AlgorithmIdentifiers, JsonWebSignature}

case class VC private[odyssey] (rdfModel: Model)

object VC {
  def withIssuanceDate(iss: LocalDate) = VCBuilder().withIssuanceDate(iss)
  def withExpiryDate(exp: LocalDate) = VCBuilder().withExpiryDate(exp)
  def withNamespaces(nsMappings: (String, String)*) = VCBuilder().withNamespaces(nsMappings: _*)
  def withClaims(values: (ResourceNode, URI, RDFNode)*) = VCBuilder().withStatements(values: _*)

  def fromJsonLd(jsonLd: String): VC = {
    val rdfModel = ModelFactory.createDefaultModel().read(new StringReader(jsonLd), "", "JSON-LD")
    VC(rdfModel)
  }

  def fromJws(jwsSer: String): Either[VerificationError, VC] = {

    def resolveVerificationKey(keyIdHeader: String): PublicKey = {
      KeyFoo.getPublicKeyFromRef(new URL(keyIdHeader))
    }

    val jws = new JsonWebSignature()
    jws.setCompactSerialization(jwsSer)
    jws.setAlgorithmConstraints(
      new AlgorithmConstraints(ConstraintType.WHITELIST, AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256)
    )
    jws.setKey(resolveVerificationKey(jws.getHeader("kid")))

    if (jws.verifySignature()) {
      val payload = jws.getPayload

      val rdfModel = ModelFactory.createDefaultModel().read(new StringReader(payload), "", "JSON-LD")

      Right(VC(rdfModel))
    } else {
      Left(InvalidSignature)
    }
  }
}
