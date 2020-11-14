package net.jtownson.odyssey.impl

import java.net.URI

import io.circe.Decoder.Result
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor, Json}
import net.jtownson.odyssey.DataSchema
import net.jtownson.odyssey.impl.CodecStuff.{uriDecoder, uriEncoder}

object CredentialSchemaValidation {

  val failureMessage =
    """
      |Sec 5.4: The value of the credentialSchema property MUST be one or more data schemas that provide verifiers
      |with enough information to determine if the provided data conforms to the provided schema.
      |Each credentialSchema MUST specify its type (for example, JsonSchemaValidator2018), and an id property
      |that MUST be a URI identifying the schema file.""".stripMargin

  implicit val credentialSchemaDecoder: Decoder[Seq[DataSchema]] = { (hc: HCursor) =>
    {

      lazy val fail = DecodingFailure(failureMessage, hc.history)
      lazy val failResult: Decoder.Result[Seq[DataSchema]] = Left(fail)

      hc.value.fold(
        jsonNull = failResult,
        jsonBoolean = _ => failResult,
        jsonNumber = _ => failResult,
        jsonString = _ => failResult,
        jsonArray = (s: Seq[Json]) => decodeAsArray(s),
        jsonObject = _ => dataSchemaDecoder(hc).map(Seq(_))
      )
    }
  }

  implicit val dataSchemaEncoder: Encoder[DataSchema] = (a: DataSchema) =>
    Json.obj(
      ("id", a.id.asJson(uriEncoder)),
      ("type", a.tpe.asJson)
    )

  implicit val dataSchemaDecoder: Decoder[DataSchema] = (c: HCursor) =>
    for {
      tpe <- c.downField("type").as[String]
      id <- c.downField("id").as[URI](uriDecoder)
    } yield {
      DataSchema(tpe, id)
    }

  private def decodeAsArray(s: Seq[Json], allowEmpty: Boolean = false): Result[Seq[DataSchema]] = {
    if (s.isEmpty) {
      if (!allowEmpty)
        Left(
          DecodingFailure("A credentialSchema cannot be empty and must contain one or more {id, type} objects.", List())
        )
      else
        Right(Seq.empty)
    } else {
      for {
        head <- decodeHead(s.head)
        tail <- decodeAsArray(s.tail, allowEmpty = true)
      } yield head +: tail
    }
  }

  private def decodeHead(h: Json): Either[DecodingFailure, DataSchema] = {
    h.as[DataSchema]
  }
}
