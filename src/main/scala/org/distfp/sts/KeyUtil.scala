package org.distfp.sts

import java.io.{FileWriter, Writer}
import java.security._
import java.security.spec.{X509EncodedKeySpec, PKCS8EncodedKeySpec}

import org.distfp.sts.Util2._

import scala.util.control.NonFatal
import scala.util.{Failure, Try}

object KeyUtil {
  /** Length of a key line. */
  val SIGNATURE_LINE_LENGTH = 20

  def generateKeys(privateKeyFileName: String, publicKeyFileName: String): Try[Unit] = {
    genKeyPair() recoverWith {
      case NonFatal(e) =>
        Failure(new StsException("Error generating keys: " + e.getMessage))
    }
    for (
      (privateKey, publicKey) <- genKeyPair();
      _ <- writeKey(privateKey, privateKeyFileName);
      _ <- writeKey(publicKey, publicKeyFileName)
    ) yield ()
  }

  private[sts] def genKeyPair(): Try[(PrivateKey, PublicKey)] = Try {
    val gen = KeyPairGenerator.getInstance("DSA", "SUN")
    val random = SecureRandom.getInstance("SHA1PRNG", "SUN")
    gen.initialize(1024, random)
    val kp = gen.generateKeyPair()
    (kp.getPrivate, kp.getPublic)
  }

  def writeKey(key: Key, fileName: String): Try[Unit] =
    Try { new FileWriter(fileName) } flatMap { w =>
      writeKey(key, w) map { _ => w.close() }recoverWith {
        case NonFatal(e) =>
          w.close()
          Failure(e)
      }
    } recoverWith {
      case e: StsException => Failure(e)
      case NonFatal(e) =>
        Failure(new StsException(s"Error writing key to '$fileName': ${e.getMessage}"))
    }

  def writeKey(key: Key, w: Writer): Try[Unit] = {
    val encodedKey = key.getEncoded
    val keyString = Base64Coder.encode(encodedKey)
    writeKey(keyString, w)
  }

  def writeKey(key: Array[Char], w: Writer): Try[Unit] =
    Try {
      for (i <- 0 until key.length by SIGNATURE_LINE_LENGTH) {
        w.write(key, i, Math.min(key.length - i, SIGNATURE_LINE_LENGTH))
        if (key.length - i > SIGNATURE_LINE_LENGTH)
          w.write(Util2.EOL)
      }
    } recoverWith {
      case NonFatal(e) =>
        Failure(new StsException("Error writing key: " + e.getMessage))
    }

  def readPrivateKey(fileName: String): Try[PrivateKey] =
    readFileContents(fileName, keepLines = false) map { privateKeyString =>
      KeyFactory.getInstance("DSA", "SUN").generatePrivate(new PKCS8EncodedKeySpec(Base64Coder.decode(privateKeyString)))
    } recoverWith {
      case NonFatal(e) =>
        Failure(new StsException(s"Failed reading private key file '$fileName': " + e.getMessage))
    }

  def readPublicKey(fileName: String): Try[PublicKey] =
    readFileContents(fileName, keepLines = false) map { publicKeyString =>
      KeyFactory.getInstance("DSA").generatePublic(new X509EncodedKeySpec(Base64Coder.decode(publicKeyString)))
    } recoverWith {
      case NonFatal(e) =>
        Failure(new StsException(s"Failed reading public key file '$fileName': " + e.getMessage))
    }
}
