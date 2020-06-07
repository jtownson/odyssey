package net.jtownson.odyssey

import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._

import scala.concurrent.ExecutionContext.Implicits.global

class VCJwsCodecSpec extends FlatSpec {

  "VPJwsCodec" should "parse a generated JWT" in {
    val jws = TestUtil.aCredential.toJws

    val vc: VC = VC.fromJws(TestUtil.whitelistedAlgos, TestUtil.testKeyResolver, jws).futureValue

    vc shouldBe TestUtil.aCredential.dataModel
  }

}
