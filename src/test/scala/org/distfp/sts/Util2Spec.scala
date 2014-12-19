package org.distfp.sts

import java.io.{PrintWriter, File}
import java.util.UUID

import org.specs2.mutable.Specification

import Delim._
import Util2._

class Util2Spec extends Specification {

  private def randomFileName: String = UUID.randomUUID.toString

  private def createTestFile(lines: Seq[String]): File = {
    val fileName = randomFileName
    val w = new PrintWriter(fileName)
    lines foreach w.println
    w.close()
    new File(fileName)
  }

  private def nonExistentFile(): String = {
    val fileName = randomFileName
    new File(fileName).delete()
    fileName
  }

  val testLines = Array("very short", "text, ", "just", "for testing")

  "readlines" should {
    "fail if the file does not exist" in {
      val fName = nonExistentFile()
      readLines(fName) must beFailedTry[Array[String]].withThrowable[SlmException]
    }

    "return all the lines, unchanged, if the file exists" in {
      val f = createTestFile(testLines)
      readLines(f.getName) must beSuccessfulTry[Array[String]].withValue(testLines)
      f.delete()
    }
  }

  "readFileContents" should {
    "fail if the file does not exist" in {
      val fName = nonExistentFile()
      Seq(false, true) forall { b =>
        readFileContents(fName, keepLines = b) must beFailedTry[String].withThrowable[SlmException]
      }
    }
    
    "return the contents of the file, as is, if the file exists" in {
      val contentsWithEOLs    = testLines.mkString(EOL)
      val contentsWithoutEOLs = testLines.mkString("")
      val f = createTestFile(testLines)
      readFileContents(f.getName, keepLines = true) must beSuccessfulTry[String].withValue(contentsWithEOLs)
      readFileContents(f.getName, keepLines = false) must beSuccessfulTry[String].withValue(contentsWithoutEOLs)
      f.delete()
    }
  }

  val B = beginDelim(defaultTextMarker)
  val E = endDelim(defaultTextMarker)

  "extractLines" should {
    "return no lines if the begin delimiter is not found" in {
      extractLines(Jabberwocky.all, B, E).deep must_== Jabberwocky.all
    }

    "return only the lines between the delimiters if both begin and end are found" in {
      val firstThree = Jabberwocky(0) ++ Array(B) ++ Jabberwocky(1) ++ Array(E) ++ Jabberwocky(2)
      extractLines(firstThree, B, E).deep must_== Jabberwocky(1)
    }
  }
}
