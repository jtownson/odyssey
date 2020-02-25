package net.jtownson.odyssey

import java.io.File
import java.io.File.separatorChar

import org.scalatest._

import scala.io.Source
import JsonLdApiSpec._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.language.postfixOps

class JsonLdApiSpec extends FlatSpec with Matchers {

  val manifest = Await.result(loadManifest("manifest.jsonld"), 5 minutes)

  println(manifest)
  manifest.sequence.foreach { testManifest =>
    println(testManifest)
  }
}

object JsonLdApiSpec {

  import org.scalatest.Assertions.fail
  import io.circe._, io.circe.parser._

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
  )

  case class NegativeSyntaxTest(
      id: String,
      name: String,
      purpose: Option[String]
  )

  case class IgnoredTest(
      id: String,
      tpe: String,
      name: String,
      purpose: Option[String],
      ignoredReason: String
  ) extends Test

  object ACursorValues {
    implicit class A(aCursor: ACursor) {
      def assume[T](implicit ev: Decoder[T]): T =
        aCursor.as[T].getOrElse(fail())
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
    manifestDoc(path).flatMap { doc =>
      val name = doc.downField("name").assume[String]
      val description = doc.downField("description").assume[String]
      val baseIri = doc.downField("baseIri").assume[String]

      Future
        .traverse(doc.downField("sequence").assume[List[Json]])(
          json => loadTestScenario(path, json.hcursor)
        )
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

    test.recover { case t => IgnoredTest(id,tpe, name, purpose, s"Loading failed due to exception $t") }
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
  ): Future[Test] = Future.unit.map(_ => IgnoredTest(id, tpe, name, purpose, "Test ignored"))

  def manifestFile(path: String): File = {
    new File(
      "w3c" + separatorChar + "json-ld-api" + separatorChar + "tests" + separatorChar + path
    )
  }

  def manifestSource(path: String): Future[String] = Future {
    println(s"Loading manifest at path '$path'.")
    scala.util
      .Using(Source.fromFile(manifestFile(path), "UTF-8"))(_.mkString)
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
    manifestSource(doc.downField(key).assume[String])
  }

  def maybeLoadDocAt(key: String, doc: HCursor): Future[Option[String]] = {
    doc.downField(key).assume[Option[String]].fold(Future(Option.empty[String]))(value => manifestSource(value).map(Option.apply))
  }
}
