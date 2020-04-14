package net.jtownson.odyssey.ld

import io.circe.JsonNumber
import TestUtil.resourceSource
import org.scalatest.FlatSpec
import org.scalatest.Matchers._

class JsonUtilSpec extends FlatSpec {

  "findKey" should "resolve top-level keys" in {
    val source = resourceSource("credentials.v1.jsonld")
    val json = io.circe.parser.parse(source).getOrElse(fail("Invalid test json"))

    val result = JsonUtil.findKey("@context", json)

    result.isDefined shouldBe true
    result.get.isObject shouldBe true
  }

  it should "resolve nested keys" in {
    val source = resourceSource("credentials.v1.jsonld")
    val json = io.circe.parser.parse(source).getOrElse(fail("Invalid test json"))

    val result = JsonUtil.findKey("@version", json)

    result.isDefined shouldBe true
    result.get.isNumber shouldBe true
    result.get.as[JsonNumber].getOrElse(fail("Invalid result")) shouldBe JsonNumber.fromString("1.1").get
  }
}
