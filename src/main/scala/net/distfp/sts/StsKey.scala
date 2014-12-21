package net.distfp.sts

import java.io._
import java.security._
import java.security.spec.{X509EncodedKeySpec, PKCS8EncodedKeySpec}


import scala.util.control.NonFatal
import scala.util.{Failure, Try}

import StsControl._
import StsIO._

object StsKey {
  /** Length of a key line. */
  val SIGNATURE_LINE_LENGTH = 20

  def generateKeys(privateKeyFileName: String, publicKeyFileName: String): Try[Unit] = {
    genKeyPair() recoverWith {
      case NonFatal(e) =>
        Failure(new StsException("Error generating keys", e))
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
      writeKey(key, w) thenAlways { w.close() }
    }

  def writeKey(key: Key, w: Writer): Try[Unit] = {
    val encodedKey = key.getEncoded
    val keyString = Base64Coder.encode(encodedKey)
    writeKey(keyString, w)
  }

  def writeKey(key: Array[Char], w: Writer): Try[Unit] =
    Try {
      key.grouped(SIGNATURE_LINE_LENGTH) foreach { chars =>
        w.write(chars)
        if (chars.length == SIGNATURE_LINE_LENGTH)
          w.write(StsIO.EOL)
      }
    } recoverWith {
      case NonFatal(e) =>
        Failure(new StsException("Error writing key", e))
    }

  abstract class KeyReader[K <: Key] {
    def readKey(r: BufferedReader): Try[K]
  }

  implicit object PrivateKeyReader extends KeyReader[PrivateKey] {
    def readKey(r: BufferedReader): Try[PrivateKey] =
      readContents(r, keepLines = false) map { privateKeyString =>
        KeyFactory.getInstance("DSA", "SUN").generatePrivate(new PKCS8EncodedKeySpec(Base64Coder.decode(privateKeyString)))
      } recoverWith {
        case NonFatal(e) =>
          Failure(new StsException("Failed reading private key", e))
      }
  }

  implicit object PublicKeyReader extends KeyReader[PublicKey] {
    def readKey(r: BufferedReader): Try[PublicKey] =
      readContents(r, keepLines = false) map { publicKeyString =>
        KeyFactory.getInstance("DSA").generatePublic(new X509EncodedKeySpec(Base64Coder.decode(publicKeyString)))
      } recoverWith {
        case NonFatal(e) =>
          Failure(new StsException("Failed reading public key", e))
      }
  }

  def readKey[K <: Key](fileName: String)(implicit kr: KeyReader[K]): Try[K] =
    Try { new BufferedReader(new FileReader(fileName)) } recoverWith {
      case NonFatal(e) => Failure(new StsException("Key file '$fileName' not found", e))
    }  flatMap { r =>
      kr.readKey(r) thenAlways { r.close() }
    }

  def readKey[K <: Key](r: BufferedReader)(implicit kr: KeyReader[K]): Try[K] =
    kr.readKey(r)

  def readKeyFromResource[K <: Key](resourceName: String, classLoader: ClassLoader = getClass.getClassLoader)
                                   (implicit kr: KeyReader[K]): Try[K] =
    classLoader.getResourceAsStream(resourceName) match {
      case null   => Failure(new StsException(s"Key resource '$resourceName' not found"))
      case stream => Try { new BufferedReader(new InputStreamReader(stream)) } flatMap { r => kr.readKey(r) }
    }
}
