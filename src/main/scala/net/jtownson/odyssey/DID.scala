package net.jtownson.odyssey

import java.net.{URL, URLConnection, URLStreamHandler, URLStreamHandlerFactory}

case class DID private (value: URL)

object DID {

  // define did: as a valid URL prefix and provide the JDK with
  // a way to resolve did: URLs.
  URL.setURLStreamHandlerFactory(new DIDStreamHandlerFactory())

  def apply(method: String, suffix: String): DID = {
    DID(new URL(s"did:$method:$suffix"))
  }

  class DIDConnection(url: URL) extends URLConnection(url) {
    override def connect(): Unit = {
      System.out.println("Connected!");
    }
  }

  class DIDStreamHandler extends URLStreamHandler {
    override def openConnection(url: URL): URLConnection = {
      new DIDConnection(url)
    }
  }

  class DIDStreamHandlerFactory extends URLStreamHandlerFactory {
    override def createURLStreamHandler(protocol: String): URLStreamHandler = {
      if ("did" == protocol) {
        new DIDStreamHandler()
      } else {
        null
      }
    }
  }
}
