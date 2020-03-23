package net.jtownson.odyssey.ld

import io.circe._
import net.jtownson.odyssey.ld.JsonLdProcessor.JsonLd.JsonLdObject
import net.jtownson.odyssey.ld.JsonLdProcessor.{JsonLd, JsonLdOptions, RdfDataset}

import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source

class JsonLdProcessorImpl(implicit ec: ExecutionContext) extends JsonLdProcessor {

  override def compact(
      input: JsonLd,
      context: Option[JsonLd],
      options: Option[JsonLdOptions]
  ): Future[JsonLd] = ???

  override def expand(input: JsonLd, options: Option[JsonLdOptions]): Future[Seq[JsonLd]] = ???

  override def flatten(
      input: JsonLd,
      context: Option[JsonLd],
      options: Option[JsonLdOptions]
  ): Future[JsonLd] = ???

  override def fromRdf(input: RdfDataset, options: Option[JsonLdOptions]): Future[Seq[JsonLd]] = ???

  override def toRdf(input: JsonLd, options: Option[JsonLdOptions]): Future[RdfDataset] = ???

  case class ParseProgress(context: Map[String, String], jsonLd: JsonLd = null, node: JsonLd = null)

  override def parse(input: Source): Future[JsonLd] = {
    val jsonStr = input.mkString
    Future
      .successful(parser.parse(jsonStr))
      .flatMap(
        (jsonParse: Either[ParsingFailure, Json]) =>
          jsonParse.toTry
            .fold(Future.failed, parse)
            .map((ldParse: Either[DecodingFailure, JsonLd]) => ldParse.toTry.fold(Future.failed, Future.successful))
      )
      .flatten
  }

  private def parseJson(json: Json): ParseProgress = {
    doFold("", ParseProgress(Map()), json)
  }

  private def parseJson(key: String, acc: ParseProgress)(json: Json): ParseProgress = {
    doFold(key, acc, json)
  }

  private def parseObj(key: String, acc: ParseProgress)(obj: JsonObject): ParseProgress = {
    obj.toIterable.foldLeft(acc) { (acc, next) =>
      val (nextKey, nextJson) = next
      doFold(nextKey, acc, nextJson)
    }
  }

  private def doFold(key: String, acc: ParseProgress, json: Json): ParseProgress = {
    json.fold(
      jsonNull = parseNull(key, acc),
      jsonBoolean = parseBoolean(key, acc),
      jsonNumber = parseNum(key, acc),
      jsonString = parseStr(key, acc),
      jsonArray = parseArr(key, acc),
      jsonObject = parseObj(key, acc)
    )
  }

  private def parseStr(key: String, acc: ParseProgress)(str: String): ParseProgress = {
    acc
  }

  private def parseNum(key: String, acc: ParseProgress)(num: JsonNumber): ParseProgress = {
    acc
  }

  private def parseArr(key: String, acc: ParseProgress)(arr: Vector[Json]): ParseProgress = {
    arr.foldLeft(acc)((acc2, next) => parseJson(key, acc2)(next))
  }

  private def parseNull(key: String, acc: ParseProgress): ParseProgress = {
    acc
  }

  private def parseBoolean(key: String, acc: ParseProgress)(b: Boolean): ParseProgress = {
    acc
  }

  private def parse(json: Json): Future[Either[DecodingFailure, JsonLd]] = {
    println(parseJson(json))
    Future.successful(Right(JsonLdObject()))
  }
}
