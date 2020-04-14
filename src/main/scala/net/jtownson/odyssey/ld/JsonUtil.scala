package net.jtownson.odyssey.ld

import io.circe.{Decoder, Json}

object JsonUtil {

  def findKeyAs[T: Decoder](key: String, json: Json): Option[T] = {
    findKey(key, json).flatMap(json => json.as[T].map(t => Some(t)).getOrElse(None))
  }

  def findKey(key: String, json: Json): Option[Json] = {
    def findLateral(iter: Iterator[(String, Json)]): Option[Json] = {
      if (iter.hasNext) {
        val (nextKey, nextJson) = iter.next
        if (nextKey == key) {
          Some(nextJson)
        } else {
          val nf = findLateral(iter)
          if (nf.isDefined) {
            nf
          } else {
            findKey(key, nextJson)
          }
        }
      } else {
        None
      }
    }
    json.fold(
      jsonNull = None,
      jsonBoolean = _ => None,
      jsonNumber = _ => None,
      jsonString = _ => None,
      jsonArray = _ => None,
      jsonObject = jsonObj => findLateral(jsonObj.toMap.iterator)
    )
  }

}
