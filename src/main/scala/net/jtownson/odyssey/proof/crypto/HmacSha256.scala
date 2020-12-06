package net.jtownson.odyssey.proof.crypto

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object HmacSha256 {

  def sign(data: Array[Byte], secret: Array[Byte]): Array[Byte] = {
    val key = new SecretKeySpec(secret, "HmacSHA256")
    val shahmac: Mac = Mac.getInstance("HmacSHA256")
    shahmac.init(key)
    shahmac.doFinal(data)
  }

  def verify(data: Array[Byte], signature: Array[Byte], secret: Array[Byte]): Boolean = {
    sign(data, secret).toSeq == signature.toSeq
  }
}
