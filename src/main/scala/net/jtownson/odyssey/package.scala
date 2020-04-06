package net.jtownson

import java.net._

import net.jtownson.odyssey.RDFNode.ResourceNode
import net.jtownson.odyssey.RDFNode.ResourceNode.URINode

package object odyssey {

  object syntax {
    implicit def did2RDFNode(did: DID): RDFNode = {
      URINode(did.value.toURI)
    }
    implicit def url2RDFNode(url: URL): RDFNode = {
      URINode(url.toURI)
    }
    implicit def uri2ResourceNode(uri: URI): ResourceNode = {
      URINode(uri)
    }

    implicit def str2URI(s: String): URI = {
      new URI(s)
    }
    implicit def str2URL(s: String): URL = {
      new URL(s)
    }
  }
}
