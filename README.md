### Odyssey

Odyssey is an implementation of the W3C [verifiable credentials data model](https://www.w3.org/TR/vc-data-model/).
It allows you to generate and parse/verify verifiable credentials.

The implementation is currently at an early stage and support is provided for the 'basic' w3c data model tests.

The library will generate and verify credentials using the JWTs signature scheme.
It does not process embedded JSON-LD proofs (yet). More to come.

The w3c vc-test-suite is included as a submodule of this project.
You can run the test suite against odyssey.
Firstly, create a config.json file under w3c/vc-test-suite (as described in the readme there). Set the following two
entries in that config:
```json
{
   "generator": "../../vc.sh",
   "presentationGenerator": "../../vp.sh",
   // other config...
}
```
Now build the odyssey jar file, which `vc.sh` will execute:
```shell script
odyssey$ sbt assembly
```
Finally, cd to the test suite directory and run the tests
```shell script
odyssey$ cd w3c/vc-test-suite
vc-test-suite$ npm install && npm test 
```

### Example usage
```scala

  import odyssey._
  import syntax._

  import syntax._

  val (publicKeyRef, privateKey): (URL, PrivateKey) = KeyFoo.getKeyPair

  // Build the datamodel
  val vc = VC()
    .withAdditionalType("AddressCredential")
    .withAdditionalContext("https://www.w3.org/2018/credentials/examples/v1")
    .withId("https://www.postoffice.co.uk/addresses/1234")
    .withIssuer("https://www.postoffice.co.uk")
    .withIssuerAttributes("contact" -> "https://www.postoffice.co.uk/contact-us")
    .withIssuanceDate(LocalDate.of(2020, 1, 1).atStartOfDay())
    .withExpirationDate(LocalDate.of(2021, 1, 1).atStartOfDay())
    .withSubjectAttributes(
      "id" -> "did:ata:abc123",
      "name" -> "Her Majesty The Queen",
      "jobTitle" -> "Queen",
      "address" -> "Buckingham Palace, SW1A 1AA"
    )
    .withEs256Signature(publicKeyRef, privateKey)

  println(s"The public key reference for verification is $publicKeyRef")

  // To send it somewhere else, we will serialize to JWS...
  val jws: String = vc.toJws.compactSerializion

  println("Generated JWS for wire transfer: ")
  println(jws)

  // ... somewhere else, another app, another part of the system, we obtain the jws...
  val publicKeyResolver: PublicKeyResolver = (publicKeyRef: URL) =>
    Future.successful(KeyFoo.getPublicKeyFromRef(publicKeyRef))
  val verifier = new Es256Verifier(publicKeyResolver)

  val parseResult: VCDataModel = VCDataModel.fromJwsCompactSer(verifier, jws).futureValue

  println(s"Received dataset has a valid signature and decodes to the following dataset:")
  import net.jtownson.odyssey.impl.VCJsonCodec
  println(VCJsonCodec.vcJsonEncoder(parseResult).printWith(Printer.spaces2))
```

### Project direction
The first steps for the project will be to fully support the credentials data model.

Next steps will be to provide better support for more signature and revocation schemes.
