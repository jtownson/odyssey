package net.jtownson.odyssey

import java.io.{StringReader, StringWriter}
import java.nio.charset.StandardCharsets.UTF_8
import java.time.LocalDateTime

import com.apicatalog.jsonld.JsonLd
import com.apicatalog.jsonld.document.{Document, JsonDocument}
import com.apicatalog.rdf.RdfDataset
import com.apicatalog.rdf.io.nquad.NQuadsWriter
import io.circe.{Json, Printer}
import io.setl.rdf.normalization.RdfNormalize
import net.jtownson.odyssey.Signer.Es256Signer
import net.jtownson.odyssey.TestUtil.aCredential
import net.jtownson.odyssey.impl.CodecStuff
import org.scalatest.FlatSpec
import org.scalatest.concurrent.ScalaFutures._
import io.circe.syntax._
import net.jtownson.odyssey.impl.CodecStuff.dfRfc3339
import CodecStuff._
import scala.concurrent.ExecutionContext.Implicits.global

class JsonLdSpec extends FlatSpec {

  "It" should "create a normalized output" in {
    val jsonToSign = aCredential.dataModel.toJson

    val c = Printer.spaces2.print(jsonToSign)
    val document: Document = JsonDocument.of(new StringReader(c))

    val sw1 = new StringWriter()
    val sw2 = new StringWriter()
    val w1 = new NQuadsWriter(sw1)
    val w2 = new NQuadsWriter(sw2)

    val rdf: RdfDataset = JsonLd.toRdf(document).get()
    val normalizedData: RdfDataset = RdfNormalize.normalize(rdf)

    w1.write(rdf)

    w2.write(normalizedData)

    val sigFormula = sw2.toString.getBytes(UTF_8)
    val (keyRef, privateKey) = KeyFoo.getKeyPair
    val signer = new Es256Signer(keyRef, privateKey)
    val sig = signer.sign(sigFormula).futureValue

    val jws = Jws()
      .withHeader("alg", "ES256K")
      .withSignature(sig)
      .compactSerializion
      .futureValue

    println(jws)

    val proof = Json.obj(
      "type" -> "Ed25519Signature2018".asJson,
      "created" -> dfRfc3339.format(LocalDateTime.now()).asJson,
      "verificationMethod" -> keyRef.asJson,
      "proofPurpose" -> "assertionMethod".asJson,
      "jws" -> jws.asJson
    )

    val jsonSigned = jsonToSign.asObject.get.add("proof", proof)

    println(Printer.spaces2.print(jsonSigned.asJson))
  }
}
