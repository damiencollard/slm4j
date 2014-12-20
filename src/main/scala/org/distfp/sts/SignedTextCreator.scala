package org.distfp.sts

import java.io._
import java.security._

import org.distfp.sts.Delim._
import org.distfp.sts.KeyUtil._
import org.distfp.sts.Util._

import scala.util.control.NonFatal
import scala.util.{Failure, Try}

import Control._

class SignedTextCreator {
  def signLicense(licenseFileName: String, privateKeyFileName: String, outputFileName: String,
                  textMarker: String = defaultTextMarker): Try[Unit] = {
    def doSign() = Try { new FileWriter(outputFileName) } flatMap { writer =>
      signLicense(licenseFileName, privateKeyFileName, writer, textMarker) thenAlways { writer.close() }
    }
    for (_ <- checkPresent(licenseFileName);
         _ <- checkPresent(privateKeyFileName);
         _ <- checkAbsent(outputFileName);
         _ <- doSign())
    yield ()
  }

  def signLicense(licenseFileName: String, privateKeyFileName: String, w: Writer,
                  textMarker: String): Try[Unit] = {
    for (
      privateKey   <- readKey[PrivateKey](privateKeyFileName);
      unsignedText <- readUnsignedText(licenseFileName);
      signedText   <- signText(unsignedText, privateKey);
      _            <- writeSignedText(signedText, w, textMarker)
    ) yield ()
  }

  private def signText(unsignedText: UnsignedText, privateKey: PrivateKey): Try[SignedText] = Try {
    val signature = Signature.getInstance("SHA1withDSA", "SUN")
    signature.initSign(privateKey)
    unsignedText.lines foreach { line =>
      signature.update(line.getBytes, 0, line.getBytes.length)
    }
    SignedText(unsignedText.lines, signature.sign())
  } recoverWith {
    case NonFatal(e) =>
      Failure(new StsException("Failed signing text", e))
  }

  private def readUnsignedText(fileName: String): Try[UnsignedText] =
    readLines(fileName) flatMap { lines => Try { UnsignedText(lines) } }

  private def writeSignedText(signedText: SignedText, w: Writer, textMarker: String): Try[Unit] =
    if (textMarker == signatureMarker)
      Failure(new StsException("Text marker cannot be identical to signature marker"))
    else Try {
      val base64Sig = Base64Coder.encode(signedText.signature)

      w.write(beginDelim(textMarker))
      w.write(EOL)
      signedText.lines foreach { line =>
        w.write(line)
        w.write(EOL)
      }
      w.write(endDelim(textMarker))
      w.write(EOL)

      w.write(signatureBegin)
      w.write(EOL)

      // Write using same formatting as for keys.
      KeyUtil.writeKey(base64Sig, w)
      w.write(EOL)
      w.write(signatureEnd)
    }
}