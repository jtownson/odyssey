package net.jtownson.odyssey

import scala.util.{Failure, Success, Try}

object Using {
  def apply[R <: AutoCloseable, A](resource: => R)(f: R => A): Try[A] = {
    try {
      Success(f(resource))
    } catch {
      case t: Throwable =>
        Failure(t)
    }
  }
}
