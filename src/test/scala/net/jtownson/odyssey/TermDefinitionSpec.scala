package net.jtownson.odyssey

import java.net.URI

import net.jtownson.odyssey.TermDefinition.URITermDefinition
import org.scalatest.FlatSpec
import org.scalatest.Matchers._

class TermDefinitionSpec extends FlatSpec {

  "URITermDefinition" should "resolve in main scenario" in {
    URITermDefinition(new URI("http://schema.org")).resolve("name") shouldBe new URI("http://schema.org/name")
  }

  it should "resolve correctly with trailing slashes" in {
    URITermDefinition(new URI("http://schema.org/")).resolve("name") shouldBe new URI("http://schema.org/name")
  }
}
