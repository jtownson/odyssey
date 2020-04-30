### Odyssey

Odyssey is an implementation of the W3C [verifiable credentials data model](https://www.w3.org/TR/vc-data-model/).
It allows you to generate and parse/verify verifiable credentials.

The implementation is currently at an early stage and support is provided for the 'basic' w3c data model tests.
More to come.

The library will generate and verify credentials provided as JWTs but it does not processes embedded JSON-LD proofs (yet).
Again, more to come.

The w3c vc-test-suite is included as a submodule of this project. You can run the test suite against odyssey as follows:
```shell script
odyssey$ sbt assembly
odyssey$ cd w3c/vc-test-suite
vc-test-suite$ npm install && npm test 
```

### Example usage
```scala

  import odyssey._
  import syntax._

  val (publicKeyRef, privateKey): (URL, PrivateKey) = KeyFoo.getKeyPair

  val vc = VC()
    .withAdditionalType("AddressCredential")
    .withAdditionalContext("https://www.w3.org/2018/credentials/examples/v1")
    .withId("https://www.postoffice.co.uk/addresses/1234")
    .withIssuer("https://www.postoffice.co.uk")
    .withIssuanceDate(LocalDate.of(2020, 1, 1).atStartOfDay())
    .withExpirationDate(LocalDate.of(2021, 1, 1).atStartOfDay())
    .withCredentialSubject(
      ("id", "did:ata:abc123"),
      ("name", "Her Majesty The Queen"),
      ("jobTitle", "Queen"),
      ("address", "Buckingham Palace, SW1A 1AA")
    )
    .withEcdsaSecp256k1Signature2019(publicKeyRef, privateKey)

  println(s"The public key reference for verification is $publicKeyRef")

  // To send it somewhere else, we will serialize
  // to JWS...
  val jws: String = vc.toJws

  println("Generated JWS for wire transfer: ")
  println(jws)

  // ... somewhere else, another app, another part of the system, we obtain the jws...
  val parseResult: VC = VC.fromJws(whitelistedAlgos, dummyKeyResolver, jws).futureValue

  println(s"Received dataset has a valid signature and decodes to the following dataset:")
  println(VCJsonCodec.vcJsonEncoder(parseResult).printWith(Printer.spaces2))

```

### Project direction
The first steps for the project will be to fully support the credentials data model.

Once complete, I will provide better support for integrating with self-sovereign identity
(this integration is currently provided via the `PublicKeyResolver` trait, but the library 
provides no useful implementations. Namely:
 * a key resolver that works with the universal DID resolver
 * one that supports the DID 'key' method.
 
When complete, I will provide support for arbitrary signed content and claims.

At that point, the library should be useful for applications needing DIDComm, DID auth, etc
and provide a way into these for developers coding in the scala ecosystem. 
