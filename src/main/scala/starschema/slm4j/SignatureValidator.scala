package starschema.slm4j

import java.security._
import java.security.spec.X509EncodedKeySpec

import scala.util.{Success, Try, Failure}
import scala.util.control.NonFatal

object SignatureValidator {
  sealed trait SignatureVerification
  case class SignatureMatch(lines: Array[String]) extends SignatureVerification
  case object SignatureMismatch extends SignatureVerification
}

class SignatureValidator {
  import SignatureValidator._

  private def extractLicense(lines: Array[String]): Try[Array[String]] =
    Success(Util2.extractLines(lines, Delim.LICENSE_BEGIN, Delim.LICENSE_END))

  private def extractSignature(lines: Array[String], _publicKey: PublicKey): Try[Array[Byte]] = Try {
    val sig = Signature.getInstance("SHA1withDSA")
    sig.initVerify(_publicKey)

    val sigLines = Util2.extractLines(lines, Delim.SIGNATURE_BEGIN, Delim.SIGNATURE_END)
    val sb = new StringBuilder
    sigLines foreach sb.append

    Base64Coder.decode(sb.toString())
  } recoverWith {
    case NonFatal(e) =>
      Failure(new SlmException("Error initializing signature: " + e.getMessage))
  }

  private def verifyTextSignature(lines: Array[String], sig: Array[Byte], _publicKey: PublicKey): Try[Boolean] =
    Try {
      val computedSig = Signature.getInstance("SHA1withDSA")
      computedSig.initVerify(_publicKey)
      lines foreach { line => computedSig.update(line.getBytes, 0, line.getBytes.length) }
      computedSig.verify(sig)
    } recoverWith {
      case NonFatal(e) =>
        Failure(new SlmException("Failed verifying signature: " + e.getMessage))
    }

  def readPublicKey(fileName: String): Try[PublicKey] =
    Util2.readFileContents(fileName, keepLines = false) map { publicKeyString =>
      KeyFactory.getInstance("DSA").generatePublic(
          new X509EncodedKeySpec(Base64Coder.decode(publicKeyString)))
    } recoverWith {
      case NonFatal(e) =>
        Failure(new SlmException("Error reading public key file: " + e.getMessage))
    }

  /** Verifies a signed license file against a public key.
   * Returns the license text lines on success.
   */
  def verifyLicense(publicKeyFile: String, signedFile: String): Try[SignatureVerification] =
    for (
      _publicKey   <- readPublicKey(publicKeyFile);
      lines        <- Util2.readLines(signedFile);
      licenseLines <- extractLicense(lines);
      licenseSig   <- extractSignature(lines, _publicKey);
      ok            <- verifyTextSignature(licenseLines, licenseSig, _publicKey)
    ) yield {
      if (ok) SignatureMatch(licenseLines) else SignatureMismatch
    }
}
