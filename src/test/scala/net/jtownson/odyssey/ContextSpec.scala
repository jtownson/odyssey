package net.jtownson.odyssey

import io.circe.Json
import net.jtownson.odyssey.Context.TermDefinition
import net.jtownson.odyssey.TestUtil.resourceSource
import org.scalatest.FlatSpec
import org.scalatest.Matchers._

class ContextSpec extends FlatSpec {

  "Context" should "resolve absolute URI terms from the credentials context" in {
    val source = resourceSource("credentials.v1.jsonld")
    val context = Context.fromString(source)

    val term = context.resolve("credentialSubject")

    term shouldBe Some("https://www.w3.org/2018/credentials#credentialSubject")
  }

  it should "resolve terms expressed as curies" in {
    val source = resourceSource("credentials.v1.jsonld")
    val context = Context.fromString(source)

    val term = context.resolve("credentialStatus")

    term shouldBe Some("https://www.w3.org/2018/credentials#credentialStatus")
  }

  it should "resolve shorthand terms" in {
    val source = resourceSource("credentials.v1.jsonld")
    val context = Context.fromString(source)

    val term = context.resolve("id")

    term shouldBe Some("@id")
  }

  it should "resolve terms relative to a @vocab" in {
    val context = Context.fromString(s"""{
          |"@context": {
          |    "@vocab": "http://example.com/vocab/"
          |  }
          |}""".stripMargin)

    val term = context.resolve("name")

    term shouldBe Some("http://example.com/vocab/name")
  }

  it should "nullify vocab terms is they are nullified" in {
    val context = Context.fromString(s"""{
         |"@context": {
         |     "@vocab": "http://example.com/vocab/",
         |     "databaseId": null
         |  }
         |}""".stripMargin)

    val term = context.resolve("databaseId")

    term shouldBe None
  }

  it should "expand a relative URI against a @base" in {
    val context = Context.fromString(
      """{
         |  "@context": {
         |    "@base": "http://example.com/base/",
         |    "label": "http://www.w3.org/2000/01/rdf-schema#label"
         |  },
         |  "@id": "",
         |  "label": "Just a simple document"
         |}""".stripMargin
    )

    val expansion = context.expand("rel")

    expansion shouldBe "http://example.com/base/rel"
  }

  "Null context" should "not resolve anything but not fail either" in {
    val context = new Context(Json.Null)

    context.resolve("term") shouldBe None
  }

  "Local context" should "be used to find items not in the parent context" in {
    val parentCtx = Context.fromString("""{
       |  "@context": {
       |    "name": "http://schema.org/name"
       |  }
       |}""".stripMargin)

    val childCtx = Context.fromString("""{
       |  "@context": {
       |    "address": "http://schema.org/address"
       |  }
       |}""".stripMargin)

    val activeContext = parentCtx.pushContext(childCtx)

    parentCtx.resolveTerm("name") shouldBe Some(TermDefinition("name", "http://schema.org/name", None, None))
    parentCtx.resolve("name") shouldBe Some("http://schema.org/name")
    childCtx.resolve("address") shouldBe Some("http://schema.org/address")
//    activeContext.resolve("address") shouldBe Some("http://schema.org/address")
  }
}

object ContextSpec {}
