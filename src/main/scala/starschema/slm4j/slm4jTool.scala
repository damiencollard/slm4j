package starschema.slm4j

import scala.collection.mutable
import scala.util.{Success, Failure}
import scala.util.control.NonFatal

import SignatureValidator.{SignatureMatch, SignatureMismatch}

object Slm4jTool {
  private val ACTION_SIGN    = "sign"
  private val ACTION_VERIFY  = "verify"
  private val ACTION_GENKEYS = "genkeys"

  private val PARAM_INPUTFILE  = "--input"
  private val PARAM_OUTPUTFILE = "--output"
  private val PARAM_PUBLICKEY  = "--public-key"
  private val PARAM_PRIVATEKEY = "--private-key"
  private val PARAM_BASENAME   = "--base-name"

  def main(args: Array[String]): Unit = {
    try {
      if (args.length == 0) {
        errorExit("Error: invalid commandline")
      }
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
    val self = "slm4j.sh"
    println(s"""
      |Usage: $self <action> [parameters]
      |
      |* Sign a file using a private key:
      |
      |    $self sign --private-key <key-file> --input <in-file> --output <out-file>
      |
      |        Signs file <in-file> using the private DSA key read from <key-file> and write the
      |        result in <out-file>.
      |
      |        Lines in the input file *cannot* start with '${Delim.delimMarker}', the marker
      |        used in signed files.
      |
      |        Exit codes: 0 if the file is successfully signed, 1 on error.
      |
      |* Verify a file using a public key:
      |
      |    $self verify --public-key <key-file> --input <in-file>
      |
      |        Verifies that file <in-file> is properly signed, using the public DSA key read from
      |        <key-file>.
      |
      |        Exit codes: 0 if the license is valid, 2 if not, and 1 on error.
      |
      |* Generate a public/private key pair:
      |
      |    $self genkeys --base-name <base-name>
      |
      |        Generates private key <base-name> and public key <base-name>.pub.
      |        DO NOT SHARE THE PRIVATE KEY.
            """.stripMargin)
  }

  private def executeApplication(arguments: Array[String]): Unit = {
    // XXX Simplify the handling of parameters!
    val parameters = mutable.HashMap.empty[String, String]

    val parameterSetSign    = mutable.HashSet.empty[String]
    val parameterSetVerify  = mutable.HashSet.empty[String]
    val parameterSetGenKeys = mutable.HashSet.empty[String]

    var parameterSet: mutable.Set[String] = null

    try {
      parameterSetSign.add(PARAM_INPUTFILE)
      parameterSetSign.add(PARAM_PRIVATEKEY)
      parameterSetSign.add(PARAM_OUTPUTFILE)

      parameterSetVerify.add(PARAM_PUBLICKEY)
      parameterSetVerify.add(PARAM_INPUTFILE)

      parameterSetGenKeys.add(PARAM_BASENAME)

      arguments(0) match {
        case ACTION_SIGN    => parameterSet = parameterSetSign
        case ACTION_VERIFY  => parameterSet = parameterSetVerify
        case ACTION_GENKEYS => parameterSet = parameterSetGenKeys
        case _              => errorExit("Invalid action")
      }

      for (i <- 1 until arguments.length) {
        // XXX Simplify this!
        if (i % 2 == 1 && (!parameterSet.contains(arguments(i)) || parameters.contains(arguments(i)))) {
          errorExit(s"Invalid or duplicated parameter '${arguments(i)}'")
        }
        if (i % 2 == 0) {
          parameters += (arguments(i - 1) -> arguments(i))
        }
      }

      if (parameterSet.size != parameters.size) {
        errorExit("Invalid commandline")
      }

      if (arguments(0) == ACTION_SIGN) {
        val privateKeyFileName = parameters(PARAM_PRIVATEKEY)
        val inputFileName      = parameters(PARAM_INPUTFILE)
        val outputFileName     = parameters(PARAM_OUTPUTFILE)

        val creator = new SignatureCreator
        creator.signLicense(inputFileName, privateKeyFileName, outputFileName) match {
          case Success(()) => ()
          case Failure(e)  => errorExit(e.getMessage)
        }
      } else if (arguments(0) == ACTION_VERIFY) {
        val publicKeyFileName = parameters(PARAM_PUBLICKEY)
        val inputFileName     = parameters(PARAM_INPUTFILE)

        val validator = new SignatureValidator
        validator.verifyLicense(inputFileName, publicKeyFileName) match {
          case Success(SignatureMatch(_)) => println("License is valid."); System.exit(0)
          case Success(SignatureMismatch) => println("License is NOT valid."); System.exit(2)
          case Failure(e)                 => errorExit(e.getMessage)
        }
      } else /* ACTION_GENKEYS */ {
        val baseName = parameters(PARAM_BASENAME);
        val privateKeyFileName = baseName
        val publicKeyFileName  = baseName + ".pub"
        KeyUtil.generateKeys(privateKeyFileName, publicKeyFileName) match {
          case Success(()) => ()
          case Failure(e)  => errorExit(e.getMessage)
        }
      }
    } catch {
      case NonFatal(e) =>
        errorExit("Lcense validation failed: " + e.getMessage)
    }
  }

  private def printError(msg: => String): Unit =
    System.err.println("Error: " + msg)

  private def errorExit(msg: => String, exitCode: Int = 1): Unit = {
    printError(msg)
    System.exit(exitCode)
  }
}
