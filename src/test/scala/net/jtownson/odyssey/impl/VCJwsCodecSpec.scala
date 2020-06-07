package net.jtownson.odyssey.impl

import net.jtownson.odyssey.{TestUtil, VC}
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._

import scala.concurrent.ExecutionContext.Implicits.global

class VCJwsCodecSpec extends FlatSpec {

  "VCJwsCodec" should "parse a generated JWT" in {
    val jwsSer = TestUtil.aCredential.toJws.compactSerializion

    val vc: VC = VC.fromJwsCompactSer(TestUtil.es256Verifier, jwsSer).futureValue

    vc shouldBe TestUtil.aCredential.dataModel
  }
}
