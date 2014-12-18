package starschema.slm4j

import java.io._
import java.security._
import java.security.spec.PKCS8EncodedKeySpec

import scala.util.{Failure, Try}
import scala.util.control.NonFatal

import Delim._
import Util2._

class SignatureCreator {
  private def computeTextSignature(lines: Array[String], _privateKey: PrivateKey): Try[Signature] = Try {
    val _signature = Signature.getInstance("SHA1withDSA", "SUN")
    _signature.initSign(_privateKey)
    // FIXME This takes the LICENSE_BEGIN line into account, but I think
    // it was this way with the original library => CHECK IT!
    lines.takeWhile(_ != LICENSE_END) foreach { line =>
      _signature.update(line.getBytes, 0, line.getBytes.length)
    }
    _signature
  } recoverWith {
    case NonFatal(e) =>
      Failure(new SlmException("Error processing source file: " + e.getMessage))
  }

  private def readPrivateKey(fileName: String): Try[PrivateKey] =
    readFileContents(fileName, false) map { privateKeyString =>
      KeyFactory.getInstance("DSA", "SUN").generatePrivate(
        new PKCS8EncodedKeySpec(Base64Coder.decode(privateKeyString)))
    } recoverWith {
      case NonFatal(e) =>
        Failure(new SlmException("Error reading private key file: " + e.getMessage))
    }

  def signLicense(licenseFileName: String, privateKeyFileName: String, w: Writer): Try[Unit] = Try {
    def save(lines: Array[String], base64Sig: Array[Char]): Try[Unit] = Try {
      w.write(LICENSE_BEGIN)
      w.write(EOL)
      lines foreach { line =>
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
    for (
      lines       <- Util2.readLines(licenseFileName);
      _privateKey <- readPrivateKey(privateKeyFileName);
      sig         <- computeTextSignature(lines, _privateKey);
      base64Sig    = Base64Coder.encode(sig.sign());
      _           <- save(lines, base64Sig)
    ) yield ()
  }

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
}
