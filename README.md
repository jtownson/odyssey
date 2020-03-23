### Odyssey

Odyssey lets you create data in the form of 'claims',
which it will (a) serialize as json-ld and (b) sign,
enabling the issuance of cryptographically verifiable
claims.

Odyssey also includes the 'vc' extension package which
allows you to handle w3c verifiable credentials, which
is a specific verifiable claim format with several
useful features.

Odyssey will parse and verify json-ld claims or w3c verifiable
credentials.

### Example usage
```scala

// Make some claims
import net.jtownson.odyssey._

val subject = DID(method = "ata", suffix = "abc123")

val context = Context.fromURL("https://json-ld.org/contexts/person.jsonld")

val claims = LinkedDataset.withContext(context).withType("Person").withClaims(
  subject -> "email" -> "mailto:john.doe@example.com",
  subject -> "telephone" -> "01101 110123",
  subject -> "address" -> "Buckingham Palace, SW1A 1AA"
)

// Sign the data
import java.security.PrivateKey
import java.net.URL
val privateKey: PrivateKey = ???
val publicKeyRef = new URL("did:ata:dXNHwJ#keys-1")

val verifiableClaims = claims.withEd25519Signature2018(privateKey, publicKeyRef)

// write the claims to a circe JSON object
import net.jtownson.odyssey.circe._
val circeJsonLd = verifiableClaims.toJson

// write the claims to a String
val jsonLdString = verifiableClaims.mkString

// write the data somewhere: a file, the network, ...

// Process incoming claims
// This is a JSON document, so the usual JSON parsing options exist)
val json = io.circe.parse(jsonLdString)

// but odyssey will detect signatures and provide signature validation.
import net.jtownson.odyssey.circe._

// Returned as a Future because fetching 
// the verification key and/or the json-ld context
// may require network hops
val incomingClaimsF: Future[Claim] = Claim.fromJson(json)

incomingClaimsF.onComplete { 
  case Success(claims) =>
    // great, we're back in the saddle
  case Failure(t) =>
    // there are a variety of error scenarios, such as:
    // a (network) problem resolving the verification public key
    // an invalid JSON object
    // an invalid signature
}

```

### W3C verifiable credentials
The code is very similar to the code above, except
that verifiable credentials have a number of standardized claims,
such as issuanceDate, expiryDate and a bunch of others. These are
supported directly with method invocations similar to:

```scala
import net.jtownson.odyssey._
import net.jtownson.odyssey.vc._

val subject = DID(method = "ata", suffix = "abc123")

val context = Context.fromURL("http://schema.org")

val vc = VerifiableCredential.mergeContext(context)
  .withSubject(subject)
  .withIssuanceDate(LocalDate.now())
  .withExpiryDate(LocalDate.now().plusYears(1))
  .withClaim("email", "mailto:jeremy.townson@example.com")
  .withClaim("telephone", "01101 110123")


val circeJsonVc = vc.toJson

val incomingVcF: Future[VerifiableCredential] = VerifiableCredential.fromJson(json)

incomingVcF.onComplete { 
  case Success(claims) =>
    // great
  case Failure(t) =>
    // there are additional failure cases for verifiable credentials
    // The expiryDate may have passed for example.
}
```

The Claim DSL in the root odyssey package is essentially an RDF processor,
enabling input/output of RDF triples.

The VerifiableCredential DSL in the vc package builds on this with a pre-defined
context which includes a number of specific claims, defined in the W3C's 
 [verifiable credentials data model](https://www.w3.org/TR/vc-data-model/).

You can think of odyssey as a stack consisting of RDF -> JSON-LD -> JSON-LD signatures -> W3C verifiable credentials.
The DSL is designed to make it as easy as possible to spit out and consume W3C credentials but you can of course
use specific functionality in the individual layers if you need. 
