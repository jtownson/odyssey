package net.jtownson.odyssey.impl

import io.circe.Printer
import net.jtownson.odyssey.{TestUtil, VCDataModel}
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._

import scala.concurrent.ExecutionContext.Implicits.global

class VCJwsCodecSpec extends FlatSpec {

  "VCJwsCodec" should "parse a generated JWT" in {
    val jwsSer = TestUtil.aCredential.toJws(Printer.spaces2).futureValue.compactSerialization

    val vc: VCDataModel = VCDataModel.fromJws(TestUtil.es256Verifier, jwsSer).futureValue

    vc shouldBe TestUtil.aCredential.dataModel
  }
}
