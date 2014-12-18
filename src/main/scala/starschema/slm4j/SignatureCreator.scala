package starschema.slm4j

import java.io._
import java.security._
import java.security.spec.PKCS8EncodedKeySpec

import scala.util.{Success, Failure, Try}
import scala.util.control.NonFatal

import Delim._
import Util2._

class SignatureCreator {
  def signLicense(licenseFileName: String, privateKeyFileName: String, outputFileName: String): Try[Unit] = {
    def doSign() = Try { new FileWriter(outputFileName) } flatMap { writer =>
      signLicense(licenseFileName, privateKeyFileName, writer) map { _ => writer.close() } recoverWith {
        case NonFatal(e) =>
          writer.close()
          Failure(e)
      }
    }
    for (_ <- checkPresent(licenseFileName);
         _ <- checkPresent(privateKeyFileName);
         _ <- checkAbsent(outputFileName);
         _ <- doSign())
    yield ()
  }

  def signLicense(licenseFileName: String, privateKeyFileName: String, w: Writer): Try[Unit] = {
    for (
      privateKey   <- readPrivateKey(privateKeyFileName);
      unsignedText <- readUnsignedText(licenseFileName);
      signedText   <- signText(unsignedText, privateKey);
      _            <- writeSignedText(signedText, w)
    ) yield ()
  }

  private def signText(unsignedText: UnsignedText, privateKey: PrivateKey): Try[SignedText] = Try {
    val signature = Signature.getInstance("SHA1withDSA", "SUN")
    signature.initSign(privateKey)
    unsignedText.lines foreach { line =>
      signature.update(line.getBytes, 0, line.getBytes.length)
    }
    SignedText(unsignedText.lines, signature)
  } recoverWith {
    case NonFatal(e) =>
      Failure(new SlmException("Error processing source file: " + e.getMessage))
  }

  private def readPrivateKey(fileName: String): Try[PrivateKey] =
    readFileContents(fileName, keepLines = false) map { privateKeyString =>
      KeyFactory.getInstance("DSA", "SUN").generatePrivate(
        new PKCS8EncodedKeySpec(Base64Coder.decode(privateKeyString)))
    } recoverWith {
      case NonFatal(e) =>
        Failure(new SlmException("Error reading private key file: " + e.getMessage))
    }

  private def readUnsignedText(fileName: String): Try[UnsignedText] =
    readLines(fileName) flatMap { lines => Try { UnsignedText(lines) } }

  private def writeSignedText(signedText: SignedText, w: Writer): Try[Unit] = Try {
    val base64Sig = Base64Coder.encode(signedText.signature.sign())

    w.write(LICENSE_BEGIN)
    w.write(EOL)
    signedText.lines foreach { line =>
      w.write(line)
      w.write(EOL)
    }
    w.write(LICENSE_END)
    w.write(EOL)

    w.write(SIGNATURE_BEGIN)
    w.write(EOL)

    // Write using same formatting as for keys.
    KeyUtil.writeKey(base64Sig, w)
    w.write(EOL)
    w.write(SIGNATURE_END)
  }
}
