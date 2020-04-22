package net.jtownson.odyssey

import java.net.URL
import java.security.PublicKey

import scala.concurrent.Future

trait PublicKeyResolver {
  def resolvePublicKey(publicKeyRef: URL): Future[PublicKey]
}
