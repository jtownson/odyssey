package net.jtownson.odyssey.impl

import net.jtownson.odyssey.VerificationError.ParseError

import scala.concurrent.Future

object Futures {
  def toFuture[R](e: Either[Throwable, R]): Future[R] = {
    e.left.map(t => ParseError(t.getMessage)).fold(Future.failed, Future.successful)
  }
}
