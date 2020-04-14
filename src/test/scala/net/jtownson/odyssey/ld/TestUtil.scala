package net.jtownson.odyssey.ld

import java.io.File
import java.io.File.separatorChar

import org.scalatest.Matchers.fail

import scala.io.Source
import scala.util.Using

object TestUtil {
  def resourceFile(path: String): File = {
    new File(
      "src" + separatorChar + "test" + separatorChar + "resources" + separatorChar + path
    )
  }

  def resourceSource(path: String): String = {
    Using(Source.fromFile(resourceFile(path), "UTF-8"))(_.mkString)
      .fold[String](
        t =>
          fail(
            s"Failed to load resource file from path $path. Got exception $t."
          ),
        identity
      )
  }

}
