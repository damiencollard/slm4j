package org.distfp.sts

import scala.util.control.NonFatal
import scala.util.{Failure, Try}

object Control {
  implicit class TryThenAlways[T](t: Try[T]) {
    def thenAlways[U](code: => U): Try[T] =
      t map { r => code; r } recoverWith {
        case NonFatal(e) =>
          code
          Failure(e)
      }
  }
}
