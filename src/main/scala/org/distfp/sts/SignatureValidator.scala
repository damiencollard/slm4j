package org.distfp.sts

import java.security._
import java.security.spec.X509EncodedKeySpec

import org.distfp.sts.Delim._
import org.distfp.sts.Util2._

import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

object SignatureValidator {
  sealed trait SignatureVerification
  case class SignatureMatch(text: Text) extends SignatureVerification
  case object SignatureMismatch extends SignatureVerification
}

class SignatureValidator {
  import org.distfp.sts.SignatureValidator._

  /** Verifies a signed license file against a public key. */
  def verifyLicense(signedFileName: String, publicKeyFileName: String): Try[SignatureVerification] =
    for (
      publicKey    <- readPublicKey(publicKeyFileName);
      signedText   <- readSignedText(signedFileName, publicKey);
      ok           <- verifySignedText(signedText, publicKey)
    ) yield {
      if (ok) SignatureMatch(signedText) else SignatureMismatch
    }

  def readPublicKey(fileName: String): Try[PublicKey] =
    readFileContents(fileName, keepLines = false) map { publicKeyString =>
      KeyFactory.getInstance("DSA").generatePublic(new X509EncodedKeySpec(Base64Coder.decode(publicKeyString)))
    } recoverWith {
      case NonFatal(e) =>
        Failure(new SlmException(s"Failed reading public key file '$fileName': " + e.getMessage))
    }

  private def readSignedText(fileName: String, publicKey: PublicKey): Try[SignedText] =
    for (lines     <- readLines(fileName);
         textLines <- extractLicense(lines);
         sig       <- extractSignature(lines, publicKey))
      yield SignedText(textLines, sig)

  private def extractLicense(lines: Array[String]): Try[Array[String]] =
    Success(extractLines(lines, LICENSE_BEGIN, LICENSE_END))

  private def extractSignature(lines: Array[String], publicKey: PublicKey): Try[Array[Byte]] = Try {
    val sig = Signature.getInstance("SHA1withDSA")
    sig.initVerify(publicKey)

    val sigLines = extractLines(lines, SIGNATURE_BEGIN, SIGNATURE_END)
    val sb = new StringBuilder
    sigLines foreach sb.append

    Base64Coder.decode(sb.toString())
  } recoverWith {
    case NonFatal(e) =>
      Failure(new SlmException("Failed extracting signature: " + e.getMessage))
  }

  private def verifySignedText(signedText: SignedText, publicKey: PublicKey): Try[Boolean] =
    Try {
      val computedSig = Signature.getInstance("SHA1withDSA")
      computedSig.initVerify(publicKey)
      signedText.lines foreach { line => computedSig.update(line.getBytes, 0, line.getBytes.length) }
      computedSig.verify(signedText.signature)
    } recoverWith {
      case NonFatal(e) =>
        Failure(new SlmException("Failed verifying signed text: " + e.getMessage))
    }
}
