package net.jtownson.odyssey.ld

import com.github.jsonldjava.utils.JsonUtils
import net.jtownson.odyssey.ld.JsonLdApiSpec.manifestFile
import net.jtownson.odyssey.ld.JsonLdProcessor.JsonLd
import net.jtownson.odyssey.ld.JsonLdProcessor.JsonLd.JsonLdObject
import org.scalatest.FunSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source

class JsonLdProcessorImplSpec extends FunSpec {

  def processor: JsonLdProcessor = new JsonLdProcessorImpl

  describe("Json-ld API") {
    describe("parsing") {
      it("should not barf") {
        val file = manifestFile("examples/Sample-JSON-LD-document.jsonld")

        val result: JsonLd = processor.parse(Source.fromFile(file, "UTF-8")).futureValue

        result shouldBe JsonLdObject(Map("@context" -> JsonLdObject()))
      }

      it("should look the same as json-ld java") {
        val file = manifestFile("examples/Sample-JSON-LD-document.jsonld")

        val dict = JsonUtils.fromString(Source.fromFile(file, "UTF-8").mkString)

        val rdf = com.github.jsonldjava.core.JsonLdProcessor.toRDF(dict)
        println(dict)
        println(rdf)
      }
    }
  }
}
