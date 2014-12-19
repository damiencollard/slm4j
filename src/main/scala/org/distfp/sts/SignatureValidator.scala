package org.distfp.sts

import java.security._

import org.distfp.sts.Delim._
import org.distfp.sts.KeyUtil._
import org.distfp.sts.Util._

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
  def verifyLicense(signedFileName: String, publicKeyFileName: String,
                    textMarker: String = defaultTextMarker): Try[SignatureVerification] =
    for (
      publicKey    <- readKey[PublicKey](publicKeyFileName);
      signedText   <- readSignedText(signedFileName, publicKey, textMarker);
      ok           <- verifySignedText(signedText, publicKey)
    ) yield {
      if (ok) SignatureMatch(signedText) else SignatureMismatch
    }

  private def readSignedText(fileName: String, publicKey: PublicKey, textMarker: String): Try[SignedText] =
    for (lines     <- readLines(fileName);
         textLines <- extractText(lines, textMarker);
         sig       <- extractSignature(lines, publicKey))
      yield SignedText(textLines, sig)

  private def extractText(lines: Seq[String], textMarker: String): Try[Seq[String]] =
    if (textMarker == signatureMarker)
      Failure(new StsException("Text marker cannot be identical to signature marker"))
    else
      Success(extractLines(lines, beginDelim(textMarker), endDelim(textMarker)))

  private def extractSignature(lines: Seq[String], publicKey: PublicKey): Try[Array[Byte]] = Try {
    val sig = Signature.getInstance("SHA1withDSA")
    sig.initVerify(publicKey)

    val sigLines = extractLines(lines, signatureBegin, signatureEnd)
    val sb = new StringBuilder
    sigLines foreach sb.append

    Base64Coder.decode(sb.toString())
  } recoverWith {
    case NonFatal(e) =>
      Failure(new StsException("Failed extracting signature: " + e.getMessage))
  }

  private def verifySignedText(signedText: SignedText, publicKey: PublicKey): Try[Boolean] =
    Try {
      val computedSig = Signature.getInstance("SHA1withDSA")
      computedSig.initVerify(publicKey)
      signedText.lines foreach { line => computedSig.update(line.getBytes, 0, line.getBytes.length) }
      computedSig.verify(signedText.signature)
    } recoverWith {
      case NonFatal(e) =>
        Failure(new StsException("Failed verifying signed text: " + e.getMessage))
    }
}
