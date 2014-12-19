package org.distfp.sts

import java.io.{BufferedReader, StringReader, StringWriter}
import java.security.{PrivateKey, PublicKey}

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
  }

  "writeKey(<private-key>) . readPrivateKey" should {
    "be identity" in {
      val (privKey, pubKey) = genKeyPair().get

      val w = new StringWriter
      writeKey(privKey, w)
      w.close()
      val str = w.toString

      val r = new BufferedReader(new StringReader(str))
      readKey[PrivateKey](r) must beSuccessfulTry[PrivateKey].withValue(privKey)
    }
  }

  "writeKey(<public-key>) . readPublicKey" should {
    "be identity" in {
      val (privKey, pubKey) = genKeyPair().get

      val w = new StringWriter
      writeKey(pubKey, w)
      w.close()
      val str = w.toString

      val r = new BufferedReader(new StringReader(str))
      readKey[PublicKey](r) must beSuccessfulTry[PublicKey].withValue(pubKey)
    }
  }
}
