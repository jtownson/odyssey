package net.jtownson.odyssey
import net.jtownson.odyssey.TestUtil.aPresentation
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._

import scala.concurrent.ExecutionContext.Implicits.global

class VPJwsCodecSpec extends FlatSpec {

  "VPJwsCodec" should "parse a generated JWT" in {
    val jws = aPresentation.toJws.compactSerializion

    val parsedDatamodel: VP = VP.fromJwsCompactSer(TestUtil.es256Verifier, jws).futureValue

    parsedDatamodel shouldBe aPresentation.dataModel
  }
}
