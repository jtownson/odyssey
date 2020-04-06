package net.jtownson.odyssey

import java.net.URI

import net.jtownson.odyssey.RDFNode.ResourceNode.{BNode, URINode}

sealed trait RDFNode

object RDFNode {

  sealed trait ResourceNode extends RDFNode

  object ResourceNode {

    case class URINode(value: URI) extends ResourceNode

    case class BNode private[odyssey] (label: String) extends ResourceNode

    def fold[T](fUri: URI => T, fBNode: String => T)(node: ResourceNode): T =
      node match {
        case URINode(value) =>
          fUri(value)
        case BNode(label) =>
          fBNode(label)
      }
  }

  case class Literal(
      lexicalForm: String,
      datatype: URI = Literal.stringType,
      langOpt: Option[String] = None
  ) extends RDFNode

  object Literal {
    val stringType = new URI("xsd:string")
  }

  def fold[T](fUri: URI => T, fBNode: String => T, fLiteral: (String, URI, Option[String]) => T)(node: RDFNode): T =
    node match {
      case URINode(value) =>
        fUri(value)
      case BNode(label) =>
        fBNode(label)
      case Literal(lexicalForm, datatype, langOpt) =>
        fLiteral(lexicalForm, datatype, langOpt)
    }
}
