package org.distfp.sts

import java.io.{BufferedReader, StringReader, StringWriter}
import java.security.{Key, PrivateKey, PublicKey}

import org.specs2.mutable.Specification

import StsKey._

class StsKeySpec extends Specification {
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

  private def writeReadIsIdentity[K <: Key](key: K)(implicit kr: KeyReader[K]): Boolean = {
    val w = new StringWriter
    writeKey(key, w)
    w.close()
    val str = w.toString

    val r = new BufferedReader(new StringReader(str))
    readKey[K](r) must beSuccessfulTry[K].withValue(key)
  }

  "writeKey . read" should {
    "be identity" in {
      val (privKey, pubKey) = genKeyPair().get
      writeReadIsIdentity(privKey)
      writeReadIsIdentity(pubKey)
    }
  }

  val nonExistent = "sdkflsqkjdfmksqdjmflkj"

  "readKey" should {
    "raise StsException if the file does not exist" in {
      readKey[PrivateKey](nonExistent) must beFailedTry[PrivateKey].withThrowable[StsException]
      readKey[PublicKey](nonExistent) must beFailedTry[PublicKey].withThrowable[StsException]
    }
  }

  "readKeyFromResource" should {
    "raise StsException if the resource does not exist" in {
      readKeyFromResource[PrivateKey](nonExistent) must beFailedTry[PrivateKey].withThrowable[StsException]
      readKeyFromResource[PublicKey](nonExistent) must beFailedTry[PublicKey].withThrowable[StsException]
    }

    "read a key from resources" in {
      val pubKeyF = readKey[PublicKey]("src/test/resources/testKey.pub").get
      val pubKeyR = readKeyFromResource[PublicKey]("testKey.pub").get
      pubKeyR must_== pubKeyF

      val privKeyF = readKey[PrivateKey]("src/test/resources/testKey").get
      val privKeyR = readKeyFromResource[PrivateKey]("testKey").get
      privKeyR must_== privKeyF
    }
  }
}
