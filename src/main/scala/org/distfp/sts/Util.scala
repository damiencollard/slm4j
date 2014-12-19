package org.distfp.sts

import java.io.File

import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

object Util {
  val EOL = "\n"

  /** Returns the lines of a file. */
  def readLines(fileName: String): Try[Seq[String]] = Try {
    val source = scala.io.Source.fromFile(fileName)
    source.getLines().toList
  } recoverWith {
    case NonFatal(e) =>
      Failure(new StsException("Error reading file: " + e.getMessage))
  }

  /** Returns the contents of a file as a string.
    *
    * If keepLines is true, the newlines are included in the returned string.
    * Otherwise, they're not.
    */
  def readFileContents(fileName: String, keepLines: Boolean = true): Try[String] =
    readLines(fileName) map (_.mkString(if (keepLines) "\n" else ""))

  /** Returns the lines between the specified delimiters. */
  def extractLines(lines: Seq[String], start: String, stop: String): Seq[String] =
    lines.dropWhile(_ != start).drop(1).takeWhile(_ != stop)

  def checkPresent(fileName: String): Try[Unit] =
    if (new File(fileName).exists()) Success(()) else Failure(new StsException(s"File '$fileName' not found"))

  def checkAbsent(fileName: String): Try[Unit] =
    if (new File(fileName).exists()) Failure(new StsException(s"File '$fileName' already exists")) else Success(())
}
