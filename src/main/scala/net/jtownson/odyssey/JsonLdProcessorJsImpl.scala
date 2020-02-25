package net.jtownson.odyssey
import net.jtownson.odyssey.JsonLdProcessor.{JsonLdContext, JsonLdDictionary, JsonLdInput, JsonLdOptions, RdfDataset}

import scala.concurrent.Future

class JsonLdProcessorJsImpl extends JsonLdProcessor {
  override def compact(
      input: JsonLdInput,
      context: Option[JsonLdContext],
      options: Option[JsonLdOptions]
  ): Future[JsonLdDictionary] = ???

  override def expand(input: JsonLdInput, options: Option[JsonLdOptions]): Future[Seq[JsonLdDictionary]] = ???

  override def flatten(
      input: JsonLdInput,
      context: Option[JsonLdContext],
      options: Option[JsonLdOptions]
  ): Future[JsonLdDictionary] = ???

  override def fromRdf(input: RdfDataset, options: Option[JsonLdOptions]): Future[Seq[JsonLdDictionary]] = ???

  override def toRdf(input: JsonLdInput, options: Option[JsonLdOptions]): Future[RdfDataset] = ???
}
