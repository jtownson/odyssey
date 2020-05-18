package net.jtownson.odyssey.ld

import java.io.File
import java.io.File.separatorChar

import io.circe.Decoder.Result
import io.circe.parser
import net.jtownson.odyssey.Using
import net.jtownson.odyssey.ld.JsonLdApiSpec.Test.{IgnoredTest, PositiveEvaluationTest, PositiveSyntaxTest}
import net.jtownson.odyssey.ld.JsonLdApiSpec._
import org.scalatest.Matchers._
import org.scalatest._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.io.Source
import scala.language.postfixOps

class JsonLdApiSpec extends FlatSpec {

//  describe("compaction algorithm") {
//    withCompactManifest { manifest =>
//      manifest.sequence.foreach { test =>
//        it(s"${test.id}, ${test.name}") {}
//      }
//    }
//  }

  def expansionPositiveEval(test: PositiveEvaluationTest): Assertion = {
    if (test.id == "#t0001") {
      val jsonLd = JsonLd.fromString(test.input)

      val expand: JsonLd = jsonLd.expand

      expand.json shouldBe parser.parse(test.expect).getOrElse(fail("Invalid test data"))
    } else {
      org.scalatest.Assertions.pending
    }
  }

  def expansionPositiveSyntax(test: PositiveSyntaxTest): Assertion = {
    org.scalatest.Assertions.pending
  }

//  withExpandManifest { manifest =>
//    manifest.sequence.foreach { test =>
//      "expansion algorithm" should s"support ${test.id}, ${test.name}" in {
//        if (test.id == "#t0001") {
//          Test.fold[Assertion](
//            test => expansionPositiveEval(test),
//            test => JsonLdApiSpec.pending(test),
//            test => JsonLdApiSpec.pending(test),
//            test => JsonLdApiSpec.pending(test),
//            test => JsonLdApiSpec.pending(test)
//          )(test)
//        }
//      }
//    }
//  }
}

object JsonLdApiSpec {

  import io.circe._
  import io.circe.parser._
  import org.scalatest.Assertions.fail

  def pending(test: Test): Assertion = org.scalatest.Assertions.pending

  def withCompactManifest(testCode: TestManifest => Any): Unit = {
    testCode(Await.result(loadTestManifest("compact-manifest.jsonld"), 5 minutes))
  }

  def withExpandManifest(testCode: TestManifest => Any): Unit = {
    testCode(Await.result(loadTestManifest("expand-manifest.jsonld"), 5 minutes))
  }

  case class TopLevelManifest(
      name: String,
      description: String,
      sequence: Seq[TestManifest]
  )

  case class TestManifest(
      name: String,
      description: String,
      baseIri: String,
      sequence: Seq[Test]
  )

  sealed trait Test {
    val id: String
    val name: String
    val purpose: Option[String]
  }

  object Test {

    def fold[T](
        fPostiveEval: PositiveEvaluationTest => T,
        fNegativeEval: NegativeEvaluationTest => T,
        fPostiveSyntax: PositiveSyntaxTest => T,
        fNegativeSyntax: NegativeSyntaxTest => T,
        fIgnored: IgnoredTest => T
    )(test: Test): T =
      test match {
        case t @ PositiveEvaluationTest(id, name, purpose, input, context, expect) => fPostiveEval(t)
        case t @ NegativeEvaluationTest(id, name, purpose, input, context, expectErrorCode) => fNegativeEval(t)
        case t @ PositiveSyntaxTest(id, name, purpose) => fPostiveSyntax(t)
        case t @ NegativeSyntaxTest(id, name, purpose) => fNegativeSyntax(t)
        case t @ IgnoredTest(id, tpe, name, purpose, ignoredReason) => fIgnored(t)
      }

    case class PositiveEvaluationTest(
        id: String,
        name: String,
        purpose: Option[String],
        input: String,
        context: Option[String],
        expect: String
    ) extends Test

    case class NegativeEvaluationTest(
        id: String,
        name: String,
        purpose: Option[String],
        input: String,
        context: Option[String],
        expectErrorCode: String
    ) extends Test

    case class PositiveSyntaxTest(
        id: String,
        name: String,
        purpose: Option[String]
    ) extends Test

    case class NegativeSyntaxTest(
        id: String,
        name: String,
        purpose: Option[String]
    ) extends Test

    case class IgnoredTest(
        id: String,
        tpe: String,
        name: String,
        purpose: Option[String],
        ignoredReason: String
    ) extends Test
  }

  object ACursorValues {
    implicit class A(aCursor: ACursor) {
      def assume[T](implicit ev: Decoder[T]): T = {
        val a: Result[T] = aCursor.as[T]
        a match {
          case Left(t) =>
            fail(s"Invalid JSON assumption: ${t}")
          case Right(value) =>
            value
        }
      }
    }
  }

  import ACursorValues._

  def loadManifest(path: String): Future[TopLevelManifest] =
    manifestDoc(path).flatMap { doc =>
      val name = doc.downField("name").assume[String]
      val description = doc.downField("description").assume[String]
      Future
        .traverse(doc.downField("sequence").assume[List[String]])(
          loadTestManifest
        )
        .map(sequence => TopLevelManifest(name, description, sequence))
    }

  def loadTestManifest(path: String): Future[TestManifest] =
    manifestDoc("tests" + separatorChar + path).flatMap { doc =>
      val name = doc.downField("name").assume[String]
      val description = doc.downField("description").assume[String]
      val baseIri = doc.downField("baseIri").assume[String]

      Future
        .traverse(doc.downField("sequence").assume[List[Json]])(json => loadTestScenario(path, json.hcursor))
        .map(sequence => TestManifest(name, description, baseIri, sequence))
    }

  private def loadTestScenario(
      path: String,
      doc: HCursor
  ): Future[Test] = {

    val id = doc.downField("@id").assume[String]
    val tpe = doc.downField("@type").assume[List[String]].head
    val name = doc.downField("name").assume[String]
    val purpose = doc.downField("purpose").assume[Option[String]]

    val test = if (tpe == "jld:PositiveEvaluationTest") {
      loadPositiveEvaluationTest(id, name, purpose, doc)
    } else if (tpe == "jld:NegativeEvaluationTest") {
      loadNegativeEvaluationTest(id, name, purpose, doc)
    } else if (tpe == "jld:PositiveSyntaxTest") {
      loadPositiveSyntaxTest(id, name, purpose, doc)
    } else if (tpe == "jld:NegativeSyntaxTest") {
      loadNegativeSyntaxTest(id, name, purpose, doc)
    } else {
      fail("Unknown test type $tpe")
    }

    test.recover {
      case t =>
        println(s"Recovering test ${id} due to $t")
        IgnoredTest(
          id,
          tpe,
          name,
          purpose,
          s"Loading failed due to exception $t"
        )
    }
  }

  def loadPositiveEvaluationTest(
      id: String,
      name: String,
      purpose: Option[String],
      doc: HCursor
  ): Future[Test] =
    for {
      input <- loadDocAt("input", doc)
      context <- maybeLoadDocAt("context", doc)
      expect <- loadDocAt("expect", doc)
    } yield PositiveEvaluationTest(id, name, purpose, input, context, expect)

  def loadNegativeEvaluationTest(
      id: String,
      name: String,
      purpose: Option[String],
      doc: HCursor
  ): Future[Test] = ignoreTest(id, "NegativeEvaluationTest", name, purpose)

  def loadPositiveSyntaxTest(
      id: String,
      name: String,
      purpose: Option[String],
      doc: HCursor
  ): Future[Test] = ignoreTest(id, "PositiveSyntaxTest", name, purpose)

  def loadNegativeSyntaxTest(
      id: String,
      name: String,
      purpose: Option[String],
      doc: HCursor
  ): Future[Test] = ignoreTest(id, "NegativeSyntaxTest", name, purpose)

  def ignoreTest(
      id: String,
      tpe: String,
      name: String,
      purpose: Option[String]
  ): Future[Test] =
    Future.successful(IgnoredTest(id, tpe, name, purpose, "Test ignored"))

  def manifestFile(path: String): File = {
    new File(
      "w3c" + separatorChar + "json-ld-api" + separatorChar + path
    )
  }

  def manifestSource(path: String): Future[String] =
    Future {
      Using(Source.fromFile(manifestFile(path), "UTF-8"))(_.mkString)
        .fold[String](
          t =>
            fail(
              s"Failed to load manifest file from path $path. Got exception $t."
            ),
          identity
        )
    }

  def manifestDoc(path: String): Future[HCursor] =
    manifestSource(path).map(parse(_).getOrElse(fail()).hcursor)

  def loadDocAt(key: String, doc: HCursor): Future[String] = {
    manifestSource("tests" + separatorChar + doc.downField(key).assume[String])
  }

  def maybeLoadDocAt(key: String, doc: HCursor): Future[Option[String]] = {
    doc
      .downField(key)
      .assume[Option[String]]
      .fold(Future(Option.empty[String]))(value => manifestSource(value).map(Option.apply))
  }
}
