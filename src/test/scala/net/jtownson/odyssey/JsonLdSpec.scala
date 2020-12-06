package net.jtownson.odyssey

import java.io.{StringReader, StringWriter}
import java.nio.charset.StandardCharsets.UTF_8
import java.security.{PrivateKey, SecureRandom}
import java.time.LocalDateTime
import java.util.Base64

import com.apicatalog.jsonld.JsonLd
import com.apicatalog.jsonld.document.{Document, JsonDocument}
import com.apicatalog.rdf.RdfDataset
import com.apicatalog.rdf.io.nquad.NQuadsWriter
import io.circe.{Json, Printer}
import io.setl.rdf.normalization.RdfNormalize
import net.jtownson.odyssey.TestUtil.aCredential
import net.jtownson.odyssey.impl.CodecStuff
import org.scalatest.FlatSpec
import org.scalatest.concurrent.ScalaFutures._
import io.circe.syntax._
import net.jtownson.odyssey.impl.CodecStuff.dfRfc3339
import CodecStuff._
import net.jtownson.odyssey.proof.jws.Es256kJwsSigner
import net.jtownson.odyssey.proof.ld.Ed25519Signature2018
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.params.{
  Ed25519KeyGenerationParameters,
  Ed25519PrivateKeyParameters,
  Ed25519PublicKeyParameters
}

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
    val (keyRef, privateKey) = KeyFoo.getECKeyPair
    val signer = new Es256kJwsSigner(keyRef, privateKey)
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
      "verificationMethod" -> keyRef.asJson,
      "proofPurpose" -> "assertionMethod".asJson,
      "jws" -> jws.asJson
    )

    val jsonSigned = jsonToSign.asObject.get.add("proof", proof)

    println(Printer.spaces2.print(jsonSigned.asJson))
  }

  it should "write the keys" in {
    val random = new SecureRandom()
    val keypairGenerator = new Ed25519KeyPairGenerator()
    keypairGenerator.init(new Ed25519KeyGenerationParameters(random))
    val asymmetricCipherKeyPair: AsymmetricCipherKeyPair = keypairGenerator.generateKeyPair();
    val privateKey: Ed25519PrivateKeyParameters =
      asymmetricCipherKeyPair.getPrivate.asInstanceOf[Ed25519PrivateKeyParameters]
    val publicKey: Ed25519PublicKeyParameters =
      asymmetricCipherKeyPair.getPublic.asInstanceOf[Ed25519PublicKeyParameters]
    val privateKeyEncoded = privateKey.getEncoded
    val publicKeyEncoded = publicKey.getEncoded

    println(Base64.getUrlEncoder.encodeToString(privateKeyEncoded))
    println(Base64.getUrlEncoder.encodeToString(publicKeyEncoded))
  }

  it should "also work with the signer class" in {
    import org.bouncycastle.jce.provider.BouncyCastleProvider
    import java.security.Security
    Security.addProvider(new BouncyCastleProvider)
    val (keyRef, privateKey) = KeyFoo.getEDKeyPairJdk
    val signer = new Ed25519Signature2018(keyRef, privateKey)

    val jsonSigned = signer.sign(aCredential.dataModel).futureValue

    println(Printer.spaces2.print(jsonSigned))
  }
}
