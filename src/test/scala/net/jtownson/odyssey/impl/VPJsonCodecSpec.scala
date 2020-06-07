package net.jtownson.odyssey.impl

import net.jtownson.odyssey.TestUtil
import org.scalatest.FlatSpec
import org.scalatest.Matchers._

class VPJsonCodecSpec extends FlatSpec {

  "VPJsonCodec" should "parse generated JSON" in {
    val vp = TestUtil.aPresentation
    val json = VPJsonCodec.vpJsonEncoder(vp.dataModel)

    val parsedModel = VPJsonCodec.vpJsonDecoder(json.hcursor)

    parsedModel shouldBe Right(vp.dataModel)
  }
}
