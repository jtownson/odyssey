package net.jtownson.odyssey

import java.net.URI
import java.security.PublicKey

import scala.concurrent.Future

trait PublicKeyResolver {
  def resolvePublicKey(publicKeyRef: URI): Future[PublicKey]
}
