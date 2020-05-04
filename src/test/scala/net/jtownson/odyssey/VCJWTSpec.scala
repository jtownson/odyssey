package net.jtownson.odyssey

import org.scalatest.FlatSpec

class VCJWTSpec extends FlatSpec {

//    val value = JsonLdProcessor.toRDF(jsonLd)
//    println(value)
//      new JsonLdApi().com.github.jsonldjava.utils.JsonUtils.fromString(jsonLd)
  /*
 The following rules apply to JOSE headers in the context of this specification:

    alg MUST be set for digital signatures.
        If only the proof property is needed for the chosen signature method
        (that is, if there is no choice of algorithm within that method),
        the alg header MUST be set to none.
    kid MAY be used if there are multiple keys associated with the issuer of the JWT.
        The key discovery is out of the scope of this specification.
        For example, the kid can refer to a key in a DID document,
        or can be the identifier of a key inside a JWKS.
    typ, if present, MUST be set to JWT.

For backward compatibility with JWT processors,
the following JWT-registered claim names MUST be used instead of,
or in addition to, their respective standard verifiable credential counterparts:

    exp MUST represent the expirationDate property, encoded as a UNIX timestamp (NumericDate).
    iss MUST represent the issuer property of a verifiable credential or the holder property of a verifiable presentation.
    nbf MUST represent issuanceDate, encoded as a UNIX timestamp (NumericDate).
    jti MUST represent the id property of the verifiable credential or verifiable presentation.
    sub MUST represent the id property contained in the verifiable credential subject.
    aud MUST represent (i.e., identify) the intended audience of the verifiable presentation
        (i.e., the verifier intended by the presenting holder to receive and verify the verifiable presentation).
   */
}
