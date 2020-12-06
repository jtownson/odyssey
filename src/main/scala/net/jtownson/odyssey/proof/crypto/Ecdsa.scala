package net.jtownson.odyssey.proof.crypto

import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.security.{PrivateKey, PublicKey}

import org.bouncycastle.asn1.sec.SECNamedCurves
import org.bouncycastle.asn1.x9.X9ECParameters
import org.bouncycastle.asn1.{ASN1InputStream, ASN1Integer, DERSequenceGenerator, DLSequence}
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.params.{ECDomainParameters, ECPrivateKeyParameters, ECPublicKeyParameters}
import org.bouncycastle.crypto.signers.{ECDSASigner, HMacDSAKCalculator}

class Ecdsa(curve: X9ECParameters) {

  def this() {
    this(SECNamedCurves.getByName("secp256k1"))
  }

  def sign(data: Array[Byte], privateKey: Array[Byte]): Array[Byte] = {
    sign(
      data,
      new ECPrivateKeyParameters(
        new BigInteger(privateKey),
        new ECDomainParameters(curve.getCurve, curve.getG, curve.getN, curve.getH)
      )
    )
  }

  def sign(data: Array[Byte], privateKey: PrivateKey): Array[Byte] = {
    sign(
      data,
      new ECPrivateKeyParameters(
        new BigInteger(privateKey.getEncoded),
        new ECDomainParameters(curve.getCurve, curve.getG, curve.getN, curve.getH)
      )
    )
  }

  def sign(data: Array[Byte], privateKeyParameters: ECPrivateKeyParameters): Array[Byte] = {
    Ecdsa.sign(curve, data, privateKeyParameters)
  }

  def verify(data: Array[Byte], signature: Array[Byte], publicKey: Array[Byte]): Boolean = {
    Ecdsa.verify(curve, data, signature, publicKey)
  }

  def verify(data: Array[Byte], signature: Array[Byte], publicKey: PublicKey): Boolean = {
    Ecdsa.verify(curve, data, signature, publicKey)
  }

  def verify(data: Array[Byte], signature: Array[Byte], publicKeyParameters: ECPublicKeyParameters): Boolean = {
    Ecdsa.verify(data, signature, publicKeyParameters)
  }
}

object Ecdsa {

  def sign(curve: X9ECParameters, data: Array[Byte], privateKey: PrivateKey): Array[Byte] = {
    sign(
      curve,
      data,
      new ECPrivateKeyParameters(
        new BigInteger(privateKey.getEncoded),
        new ECDomainParameters(curve.getCurve, curve.getG, curve.getN, curve.getH)
      )
    )
  }

  def sign(curve: X9ECParameters, data: Array[Byte], privateKey: Array[Byte]): Array[Byte] = {
    sign(
      curve,
      data,
      new ECPrivateKeyParameters(
        new BigInteger(privateKey),
        new ECDomainParameters(curve.getCurve, curve.getG, curve.getN, curve.getH)
      )
    )
  }

  def sign(curve: X9ECParameters, data: Array[Byte], privateKeyParameters: ECPrivateKeyParameters): Array[Byte] = {
    val signer = new ECDSASigner(new HMacDSAKCalculator(new SHA256Digest()))
    signer.init(true, privateKeyParameters)
    toBytes(curve, signer.generateSignature(data))
  }

  def verify(curve: X9ECParameters, data: Array[Byte], signature: Array[Byte], publicKey: PublicKey): Boolean = {
    val domainParameters = new ECDomainParameters(curve.getCurve, curve.getG, curve.getN, curve.getH)
    verify(
      data,
      signature,
      new ECPublicKeyParameters(curve.getCurve.decodePoint(publicKey.getEncoded), domainParameters)
    )
  }

  def verify(curve: X9ECParameters, data: Array[Byte], signature: Array[Byte], publicKey: Array[Byte]): Boolean = {
    val domainParameters = new ECDomainParameters(curve.getCurve, curve.getG, curve.getN, curve.getH)
    verify(data, signature, new ECPublicKeyParameters(curve.getCurve.decodePoint(publicKey), domainParameters))
  }

  def verify(data: Array[Byte], signature: Array[Byte], publicKeyParameters: ECPublicKeyParameters): Boolean = {
    val asn1 = new ASN1InputStream(signature)
    val signer = new ECDSASigner()
    signer.init(false, publicKeyParameters)
    val seq = asn1.readObject().asInstanceOf[DLSequence]
    val r = seq.getObjectAt(0).asInstanceOf[ASN1Integer].getPositiveValue
    val s = seq.getObjectAt(1).asInstanceOf[ASN1Integer].getPositiveValue
    signer.verifySignature(data, r, s)
  }

  private def toBytes(curve: X9ECParameters, sig: Array[BigInteger]): Array[Byte] = {
    val baos: ByteArrayOutputStream = new ByteArrayOutputStream()
    val seq = new DERSequenceGenerator(baos)
    try {
      seq.addObject(new ASN1Integer(sig(0)))
      seq.addObject(new ASN1Integer(toCanonicalS(curve, sig(1))))
      baos.toByteArray
    } finally {
      seq.close()
    }
  }

  private def toCanonicalS(curve: X9ECParameters, s: BigInteger): BigInteger = {
    val HALF_CURVE_ORDER = curve.getN.shiftRight(1)
    if (s.compareTo(HALF_CURVE_ORDER) <= 0) {
      s
    } else {
      curve.getN.subtract(s)
    }
  }
}
