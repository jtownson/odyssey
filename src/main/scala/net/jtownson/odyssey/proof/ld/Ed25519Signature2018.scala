package net.jtownson.odyssey.proof.ld

import java.io.{StringReader, StringWriter}
import java.net.URI
import java.nio.charset.StandardCharsets.UTF_8
import java.security.PrivateKey
import java.time.LocalDateTime

import com.apicatalog.jsonld.JsonLd
import com.apicatalog.jsonld.document.{Document, JsonDocument}
import com.apicatalog.rdf.RdfDataset
import com.apicatalog.rdf.io.nquad.NQuadsWriter
import io.circe.syntax.EncoderOps
import io.circe.{Json, Printer}
import io.setl.rdf.normalization.RdfNormalize
import net.jtownson.odyssey.Jws
import net.jtownson.odyssey.impl.CodecStuff.{dfRfc3339, uriEncoder}
import net.jtownson.odyssey.proof.LdSigner
import net.jtownson.odyssey.proof.jws.Ed25519JwsSigner

import scala.concurrent.{ExecutionContext, Future}

class Ed25519Signature2018(publicKeyRef: URI, privateKey: PrivateKey)(implicit ec: ExecutionContext) extends LdSigner {

  val signer = new Ed25519JwsSigner(publicKeyRef, privateKey)

  override def ldProof(json: Json): Future[Json] = {

    val sigFormula: Array[Byte] = getSigFormula(json)

    signer.sign(sigFormula).map { sigBytes =>
      val sigJws: String = getSigJws(sigBytes)

      Json.obj(
        "type" -> "Ed25519Signature2018".asJson,
        "created" -> dfRfc3339.format(LocalDateTime.now()).asJson,
        "verificationMethod" -> publicKeyRef.asJson,
        "proofPurpose" -> "assertionMethod".asJson,
        "jws" -> sigJws.asJson
      )
    }
  }

  private def getSigJws(sigBytes: Array[Byte]): String = {
    val jwsHeaders = signer.getAlgorithmHeaders
    Jws(
      jwsHeaders,
      Printer.spaces2.print(jwsHeaders.asJson).getBytes(UTF_8),
      Array.emptyByteArray,
      sigBytes
    ).compactSerialization
  }

  private def getSigFormula(json: Json): Array[Byte] = {
    val document: Document = JsonDocument.of(new StringReader(Printer.noSpaces.print(json)))
    val rdf: RdfDataset = JsonLd.toRdf(document).get()

    val rdfURDNA2015: RdfDataset = RdfNormalize.normalize(rdf)

    val nquadsWriter = new NQuadsWriter(new StringWriter())

    nquadsWriter.write(rdfURDNA2015)

    val sigFormula = nquadsWriter.toString.getBytes(UTF_8)
    sigFormula
  }
}
