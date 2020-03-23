package net.jtownson.odyssey

import org.scalatest.FlatSpec

class UsageSpec extends FlatSpec {

  import syntax._

  val subject = DID(method = "ata", suffix = "abc123")

  val context = Context.fromURLSync("https://json-ld.org/contexts/person.jsonld")

  val claims = LinkedDataset.withContext(context).withType("Person").withClaims(
    (subject, "email", "mailto:john.doe@example.com"),
    (subject, "telephone", "01101 110123"),
    (subject, "address", "Buckingham Palace, SW1A 1AA")
  )

  val json = claims.toJson

  val str = claims.mkString



}
