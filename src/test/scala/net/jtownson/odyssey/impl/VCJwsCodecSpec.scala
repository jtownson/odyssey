package net.jtownson.odyssey.impl

import io.circe.Printer
import net.jtownson.odyssey.{TestUtil, VCDataModel}
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class VCJwsCodecSpec extends FlatSpec {
  implicit val patienceConfig: PatienceConfig = PatienceConfig(timeout = 1000 second)
  "VCJwsCodec" should "parse a generated JWT" in {
    val jwsSer = TestUtil.aCredential.toJws(TestUtil.ecJwsSigner, Printer.spaces2).futureValue.compactSerialization

    val vc: VCDataModel = VCDataModel.fromJws(TestUtil.ecJwsVerifier, jwsSer).futureValue

    vc shouldBe TestUtil.aCredential.dataModel
  }
}
