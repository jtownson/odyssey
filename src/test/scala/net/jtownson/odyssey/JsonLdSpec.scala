package net.jtownson.odyssey

import java.io.{StringReader, StringWriter}
import java.nio.charset.StandardCharsets.UTF_8
import java.time.LocalDateTime

import com.apicatalog.jsonld.JsonLd
import com.apicatalog.jsonld.document.{Document, JsonDocument}
import com.apicatalog.rdf.RdfDataset
import com.apicatalog.rdf.io.nquad.NQuadsWriter
import io.circe.syntax._
import io.circe.{Json, Printer}
import io.setl.rdf.normalization.RdfNormalize
import net.jtownson.odyssey.TestUtil.aCredential
import net.jtownson.odyssey.impl.CodecStuff.{dfRfc3339, _}
import net.jtownson.odyssey.proof.jws.Es256kJwsSigner
import org.scalatest.FlatSpec
import org.scalatest.concurrent.ScalaFutures._

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
    val (privateKey, publicKey, publicKeyId, publicKeyResolver) = KeyFoo.generateECKeyPair()
    val signer = new Es256kJwsSigner(publicKeyId, privateKey)
    val sig = signer.sign(sigFormula).futureValue

    val jwsHeaders = Map("alg" -> "ES256K".asJson)
    val jws = Jws(
      jwsHeaders.asJson.asObject.get.toMap,
      Printer.spaces2.print(jwsHeaders.asJson).getBytes(UTF_8),
      Array.emptyByteArray,
      sig
    ).compactSerialization

    println(jws)

    val proof = Json.obj(
      "type" -> "Ed25519Signature2018".asJson,
      "created" -> dfRfc3339.format(LocalDateTime.now()).asJson,
      "verificationMethod" -> publicKeyId.asJson,
      "proofPurpose" -> "assertionMethod".asJson,
      "jws" -> jws.asJson
    )

    val jsonSigned = jsonToSign.asObject.get.add("proof", proof)

    println(Printer.spaces2.print(jsonSigned.asJson))
  }

  it should "be possible to create ED25519 keys" in {
    import java.security.Security

    import org.bouncycastle.jce.provider.BouncyCastleProvider
    Security.addProvider(new BouncyCastleProvider)
    val (privateKey, publicKey, keyId, resolver) = KeyFoo.generateEDKeyPair()

    println(s"got them $privateKey, $publicKey, ${privateKey.getAlgorithm}, ${privateKey.getFormat}")
  }
}
