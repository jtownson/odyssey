package net.jtownson.odyssey.impl

import net.jtownson.odyssey.TestUtil.aPresentation
import net.jtownson.odyssey.{TestUtil, VPDataModel}
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._

import scala.concurrent.ExecutionContext.Implicits.global

class VPJwsCodecSpec extends FlatSpec {

  "VPJwsCodec" should "parse a generated JWT" in {
    val jws = aPresentation.toJws.compactSerializion.futureValue

    val parsedDatamodel: VPDataModel = VPDataModel.fromJwsCompactSer(TestUtil.es256Verifier, jws).futureValue

    parsedDatamodel shouldBe aPresentation.dataModel
  }
}
