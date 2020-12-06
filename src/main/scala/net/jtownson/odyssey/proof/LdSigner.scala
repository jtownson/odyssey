package net.jtownson.odyssey.proof

import io.circe.Json
import io.circe.syntax.EncoderOps
import net.jtownson.odyssey.{VCDataModel, VPDataModel}
import net.jtownson.odyssey.impl.VCJsonCodec.vcJsonEncoder
import net.jtownson.odyssey.impl.VPJsonCodec.vpJsonEncoder

import scala.concurrent.{ExecutionContext, Future}

abstract class LdSigner(implicit ec: ExecutionContext) {
  def sign(vp: VPDataModel): Future[Json] = {
    addLdProof(vpJsonEncoder(vp))
  }

  def sign(vc: VCDataModel): Future[Json] = {
    addLdProof(vcJsonEncoder(vc))
  }

  private def addLdProof(json: Json) = {
    // Obtain the proof and add it to the vpJson
    ldProof(json)
      .map(proof => json.asObject.get.add("proof", proof).asJson)
  }

  // This is a primitive step to produce a proof element for the vc JSON
  // 1) normalize the json to obtain the signing input
  // 2) sign according to the algo
  // 3) produce and return a JSON element containing the signature and any required headers.
  def ldProof(json: Json): Future[Json]
}
