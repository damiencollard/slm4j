package starschema.slm4j

import java.io.FileWriter
import java.io.Writer
import java.security._

import scala.util.{Failure, Try}
import scala.util.control.NonFatal

object KeyUtil2 {
  /** Length of a key line. */
  val SIGNATURE_LINE_LENGTH = 20

  def generateKeys(privateKeyFileName: String, publicKeyFileName: String): Try[Unit] = {
    def genKeyPair(): Try[(PrivateKey, PublicKey)] = Try {
      val gen = KeyPairGenerator.getInstance("DSA", "SUN")
      val random = SecureRandom.getInstance("SHA1PRNG", "SUN")

      gen.initialize(1024, random)

      val kp = gen.generateKeyPair()
      (kp.getPrivate, kp.getPublic)
    } recoverWith {
      case NonFatal(e) =>
        Failure(new SlmException("Error generating keys: " + e.getMessage))
    }
    for (
      (privateKey, publicKey) <- genKeyPair();
      _ <- writeKey(privateKey, privateKeyFileName);
      _ <- writeKey(publicKey, publicKeyFileName)
    ) yield ()
  }

  def writeKey(key: Key, fileName: String): Try[Unit] =
    Try { new FileWriter(fileName) } flatMap { w =>
      writeKey(key, w) recoverWith {
        case NonFatal(e) =>
          w.close()
          Failure(e)
      }
    } recoverWith {
      case e: SlmException => Failure(e)
      case NonFatal(e) =>
        Failure(new SlmException(s"Error writing key to '$fileName': ${e.getMessage}"))
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
        Failure(new SlmException("Error writing key: " + e.getMessage))
    }
}
