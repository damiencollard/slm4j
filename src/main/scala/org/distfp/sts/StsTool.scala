package org.distfp.sts

import org.distfp.sts.SignatureValidator.{SignatureMatch, SignatureMismatch}

import scala.util.control.NonFatal
import scala.util.{Failure, Success}

object StsTool {
  private val ACTION_SIGN    = "sign"
  private val ACTION_VERIFY  = "verify"
  private val ACTION_GENKEYS = "genkeys"

  private val PARAM_INPUTFILE  = "--input"
  private val PARAM_OUTPUTFILE = "--output"
  private val PARAM_PUBLICKEY  = "--public-key"
  private val PARAM_PRIVATEKEY = "--private-key"
  private val PARAM_BASENAME   = "--base-name"
  private val PARAM_TEXTMARKER = "--text-marker"

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
      val parameters = {
        val options = arguments.drop(1).grouped(2).toList.map(o => o(0) -> o(1))
        Map(options: _*)
      }

      def getParam(name: String): String =
        parameters.get(name) getOrElse { exitInvalid(); "" }

      def getOptParam(param: String, default: String): String =
        parameters.getOrElse(param, default)

      arguments(0) match {
        case ACTION_SIGN =>
          val privateKeyFileName = getParam(PARAM_PRIVATEKEY)
          val inputFileName      = getParam(PARAM_INPUTFILE)
          val outputFileName     = getParam(PARAM_OUTPUTFILE)
          val textMarker         = getOptParam(PARAM_TEXTMARKER, Delim.defaultTextMarker)

          val creator = new SignatureCreator
          creator.signLicense(inputFileName, privateKeyFileName, outputFileName, textMarker) match {
            case Success(()) => ()
            case Failure(e)  => errorExit(e.getMessage)
          }

        case ACTION_VERIFY =>
          val publicKeyFileName = getParam(PARAM_PUBLICKEY)
          val inputFileName     = getParam(PARAM_INPUTFILE)
          val textMarker        = getOptParam(PARAM_TEXTMARKER, Delim.defaultTextMarker)

          val validator = new SignatureValidator
          validator.verifyLicense(inputFileName, publicKeyFileName, textMarker) match {
            case Success(SignatureMatch(_)) => println("Signed text is valid."); System.exit(0)
            case Success(SignatureMismatch) => println("Signed text is NOT valid."); System.exit(2)
            case Failure(e)                 => errorExit(e.getMessage)
          }

        case ACTION_GENKEYS =>
          val baseName = parameters(PARAM_BASENAME);
          val privateKeyFileName = baseName
          val publicKeyFileName  = baseName + ".pub"
          KeyUtil.generateKeys(privateKeyFileName, publicKeyFileName) match {
            case Success(()) => ()
            case Failure(e)  => errorExit(e.getMessage)
          }

        case _ =>
          exitInvalid()
      }
    } catch {
      case NonFatal(e) =>
        errorExit("Lcense validation failed: " + e.getMessage)
    }
  }

  def exitInvalid(): Unit =
    errorExit("Invalid commandline")

  private def errorExit(msg: => String, exitCode: Int = 1): Unit = {
    printError(msg)
    System.exit(exitCode)
  }

  private def printError(msg: => String): Unit =
    System.err.println("Error: " + msg)
}
