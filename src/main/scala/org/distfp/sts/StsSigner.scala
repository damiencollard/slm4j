package org.distfp.sts

import java.io._
import java.security._

import org.distfp.sts.StsDelim._
import org.distfp.sts.StsIO._

import scala.util.control.NonFatal
import scala.util.{Failure, Try}

import StsControl._

class StsSigner {
  def signTextFile(inputFileName: String, privateKey: PrivateKey, outputFileName: String,
                   textMarker: String = defaultTextMarker): Try[Unit] = {
    def doSign() = Try { new FileWriter(outputFileName) } flatMap { writer =>
      signTextFile(inputFileName, privateKey, writer, textMarker) thenAlways { writer.close() }
    }
    for (_ <- checkPresent(inputFileName);
         _ <- checkAbsent(outputFileName);
         _ <- doSign())
      yield ()
  }

  def signTextFile(inputFileName: String, privateKey: PrivateKey, w: Writer,
                   textMarker: String): Try[Unit] = {
    for (unsignedText <- readUnsignedText(inputFileName);
         signedText   <- signText(unsignedText, privateKey);
         _            <- writeSignedText(signedText, w, textMarker))
      yield ()
  }

  def signText(unsignedText: StsUnsignedText, privateKey: PrivateKey): Try[StsSignedText] = Try {
    val signature = Signature.getInstance("SHA1withDSA", "SUN")
    signature.initSign(privateKey)
    unsignedText.lines foreach { line =>
      signature.update(line.getBytes, 0, line.getBytes.length)
    }
    StsSignedText(unsignedText.lines, signature.sign())
  } recoverWith {
    case NonFatal(e) =>
      Failure(new StsException("Failed signing text", e))
  }

  def readUnsignedText(fileName: String): Try[StsUnsignedText] =
    readLines(fileName) flatMap { lines => Try { StsUnsignedText(lines) } }

  def writeSignedText(signedText: StsSignedText, fileName: String, textMarker: String): Try[Unit] =
    Try { new FileWriter(fileName) } flatMap { w =>
      writeSignedText(signedText, w, textMarker) thenAlways { w.close() }
    }

  def writeSignedText(signedText: StsSignedText, w: Writer, textMarker: String): Try[Unit] =
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
      StsKey.writeKey(base64Sig, w)
      w.write(EOL)
      w.write(signatureEnd)
    }
}
