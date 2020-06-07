package net.jtownson.odyssey.impl

import io.circe.Json
import net.jtownson.odyssey.TestUtil.aCredential
import org.scalatest.FlatSpec
import org.scalatest.Matchers._

class VCJsonCodecSpec extends FlatSpec {
  "VCJsonCodec" should "decode an encoded credential" in {
    val vc = aCredential.dataModel
    val json: Json = VCJsonCodec.vcJsonEncoder(vc)
    val parseResult = VCJsonCodec.vcJsonDecoder(json.hcursor)
    parseResult shouldBe Right(vc)
  }
}
