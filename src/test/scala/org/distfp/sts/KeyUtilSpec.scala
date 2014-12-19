package org.distfp.sts

import java.io.StringWriter

import org.specs2.mutable.Specification

import KeyUtil._

class KeyUtilSpec extends Specification {
  "writeKey" should {
    "write the Array[Char] key to the writer" in {
      val testString = "Just testing"
      val key = testString.toCharArray
      val w = new StringWriter
      writeKey(key, w)
      w.close()
      w.toString must_== testString
    }

//    "write a PublicKey to the writer" in {
//      genKeyPair() map { case (privKey, pubKey) =>
//        val w = new StringWriter
//        writeKey(pubKey, w)
//        w.close()
//        w.toString == pubKey
//      } must beSuccessfulTry[Boolean].withValue(true)
//    }
  }
}
