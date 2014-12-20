package org.distfp.sts

import java.security.{PrivateKey, PublicKey}

import org.distfp.sts.SignedTextValidator.{SignatureMatch, SignatureMismatch}

import scala.util.control.NonFatal
import scala.util.{Failure, Success}

object StsTool {
  private val ActionSign    = "sign"
  private val ActionVerify  = "verify"
  private val ActionGenKeys = "genkeys"

  private val ParamInputFile  = "--input"
  private val ParamOutputFile = "--output"
  private val ParamPublicKey  = "--public-key"
  private val ParamPrivateKey = "--private-key"
  private val ParamBaseName   = "--base-name"
  private val ParamTextMarker = "--text-marker"

  def main(args: Array[String]): Unit = {
    try {
      if (args.length == 0)
        exitInvalid()
      if (args.contains("-h") || args.contains("--help"))
        printUsage()
      else {
        executeApplication(args)
      }
    } catch {
      case NonFatal(e) =>
        System.err.println(e.getMessage)
    }
  }

  private def printUsage(): Unit = {
    val self = "sts.sh"
    println(s"""
      |Usage: $self <action> [parameters]
      |
      |* Sign a file using a private key:
      |
      |    $self sign --private-key <key-file> --input <in-file> --output <out-file>
      |               [--text-marker <marker>]
      |
      |        Signs file <in-file> using the private DSA key read from <key-file>
      |        and write the result in <out-file>.
      |
      |        Lines in the input file *cannot* start with '${Delim.delimSeparator}' as it's
      |        the delimiter used to separate text from signature in signed files.
      |
      |        You can optionally specify the marker to use in the text delimiters.
      |        The default is "${Delim.defaultTextMarker}". It can't be identical to the signature
      |        marker, "${Delim.signatureMarker}".
      |
      |        Exit codes: 0 if the file is successfully signed, 1 on error.
      |
      |* Verify a file using a public key:
      |
      |    $self verify --public-key <key-file> --input <in-file> [--text-marker <marker>]
      |
      |        Verifies that file <in-file> is properly signed, using the public
      |        DSA key read from <key-file>.
      |
      |        You should specify the marker used in the text delimiters unless it's the
      |        default "${Delim.defaultTextMarker}".
      |
      |        Exit codes: 0 if the license is valid, 2 if not, and 1 on error.
      |
      |* Generate a public/private key pair:
      |
      |    $self genkeys --base-name <base-name>
      |
      |        Generates private key <base-name> and public key <base-name>.pub.
      |        DO NOT SHARE THE PRIVATE KEY.
      |""".stripMargin)
  }

  private def executeApplication(arguments: Array[String]): Unit = {
    // XXX Simplify the handling of parameters!
    //val parameters = mutable.HashMap.empty[String, String]

    try {
      if (arguments.drop(1).length % 2 != 0)
        exitInvalid()
      val params = {
        val options = arguments.drop(1).grouped(2).toList.map(o => o(0) -> o(1))
        Map(options: _*)
      }

      def getParam(name: String): String =
        params.get(name) getOrElse { exitInvalid(); "" }

      def getOptParam(param: String, default: String): String =
        params.getOrElse(param, default)

      arguments(0) match {
        case ActionSign =>
          val privateKeyFileName = getParam(ParamPrivateKey)
          val inputFileName      = getParam(ParamInputFile)
          val outputFileName     = getParam(ParamOutputFile)
          val textMarker         = getOptParam(ParamTextMarker, Delim.defaultTextMarker)

          (for (privateKey <- StsKey.readKey[PrivateKey](privateKeyFileName);
                creator     = new SignedTextCreator;
                result     <- creator.signTextFile(inputFileName, privateKey, outputFileName, textMarker))
             yield result
          ) match {
            case Success(()) => ()
            case Failure(e)  => errorExit(e)
          }

        case ActionVerify =>
          val publicKeyFileName = getParam(ParamPublicKey)
          val inputFileName     = getParam(ParamInputFile)
          val textMarker        = getOptParam(ParamTextMarker, Delim.defaultTextMarker)

          (for (publicKey <- StsKey.readKey[PublicKey](publicKeyFileName);
                validator  = new SignedTextValidator;
                result    <- validator.verifyTextFile(inputFileName, publicKey, textMarker))
             yield result
          ) match {
            case Success(SignatureMatch(_)) => println("Signed text is valid."); System.exit(0)
            case Success(SignatureMismatch) => println("Signed text is NOT valid."); System.exit(2)
            case Failure(e)                 => errorExit(e)
          }

        case ActionGenKeys =>
          val baseName = params(ParamBaseName)
          val privateKeyFileName = baseName
          val publicKeyFileName  = baseName + ".pub"
          StsKey.generateKeys(privateKeyFileName, publicKeyFileName) match {
            case Success(()) => ()
            case Failure(e)  => errorExit(e)
          }

        case _ =>
          exitInvalid()
      }
    } catch {
      case NonFatal(e) =>
        errorExit("License validation failed: " + e.getMessage)
    }
  }

  def exitInvalid(): Unit =
    errorExit("Invalid commandline")

  private def errorExit(e: Throwable): Unit =
    if (e.getCause != null) errorExit(s"${e.getMessage}: ${e.getCause.getMessage}")
    else errorExit(e.getMessage)

  private def errorExit(msg: => String): Unit = {
    printError(msg)
    System.exit(1)
  }

  private def printError(msg: => String): Unit =
    System.err.println("Error: " + msg)
}
