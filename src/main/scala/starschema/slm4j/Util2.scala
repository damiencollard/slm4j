package starschema.slm4j;

import java.util.ArrayList

import scala.util.{Failure, Try}
import scala.util.control.NonFatal

object Util2 {
  val EOL = "\n"

  /** Returns the lines of a file. */
  def readLines(fileName: String): Try[Array[String]] = Try {
    val source = scala.io.Source.fromFile(fileName)
    source.getLines.toArray
  } recoverWith {
    case NonFatal(e) =>
      Failure(new SlmException("Error reading file: " + e.getMessage))
  }

  /** Returns the contents of a file as a string.
    *
    * If keepLines is true, the newlines are included in the returned string.
    * Otherwise, they're not.
    */
  def readFileContents(fileName: String, keepLines: Boolean = true): Try[String] =
    readLines(fileName) map (_.mkString(if (keepLines) "\n" else ""))

  /** Returns the lines between the specified delimiters. */
  def extractLines(lines: Array[String], beginDelim: String, endDelim: String): Array[String] =
    lines.dropWhile(_ != beginDelim).drop(1).takeWhile(_ != endDelim)
}

