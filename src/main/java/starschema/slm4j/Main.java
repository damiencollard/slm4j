/*
 * Copyright (c) 2008, 2009, Starschema Limited
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package starschema.slm4j;

import java.io.FileWriter;
import java.util.*;

public class Main {

    private static final String ACTION_SIGN   = "sign";
    private static final String ACTION_VERIFY = "verify";

    private static final String PARAM_INPUTFILE  = "--input";
    private static final String PARAM_OUTPUTFILE = "--output";
    private static final String PARAM_PUBLICKEY  = "--public-key";
    private static final String PARAM_PRIVATEKEY = "--private-key";

    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                System.err.println("Error: invalid commandline");
                System.exit(1);
            }
            List<String> argList = Arrays.asList(args);
            if (argList.contains("-h") || argList.contains("--help"))
                printUsage();
            else {
                boolean r = executeApplication(args);
                System.exit(r ? 0 : 1);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println(ex.getMessage());
        }
    }

    private static void printUsage() {
        String self = "slm4j.sh";
        String[] helpText = {
            "Usage: " + self +" <action> [parameters]",
            "",
            "Sign a file using a private key:",
            "",
            "    " + self + " sign --private-key <key-file> --input <in-file> --output <out-file>",
            "",
            "        Sign file <in-file> using the private DSA key read from <key-file> and write the",
            "        result in <out-file>.",
            "",
            "        Exit codes: 0 if the file is successfully signed, 1 on error.",
            "",
            "    " + self + " verify --public-key <key-file> --input <in-file>",
            "",
            "        Verifies that file <in-file> is properly signed, using the public DSA key read from",
            "        <key-file>.",
            "",
            "        Exit codes: 0 if the license is valid, 2 if not, and 1 on error."
        };

        for (int i = 0; i < helpText.length; i++)
            System.out.println(helpText[i]);
    }

    public static boolean executeApplication(String[] arguments) {
        HashMap parameters = new HashMap();
        Set parameterSet;
        Set parameterSetSign = new HashSet();
        Set parameterSetVerify = new HashSet();

        try {
            parameterSetSign.add(PARAM_INPUTFILE);
            parameterSetSign.add(PARAM_PRIVATEKEY);
            parameterSetSign.add(PARAM_OUTPUTFILE);

            parameterSetVerify.add(PARAM_PUBLICKEY);
            parameterSetVerify.add(PARAM_INPUTFILE);

            if (arguments[0].toLowerCase().equals(ACTION_SIGN)) {
                parameterSet = parameterSetSign;
            } else if (arguments[0].toLowerCase().equals(ACTION_VERIFY)) {
                parameterSet = parameterSetVerify;
            } else {
                System.err.println("Invalid action");
                return false;
            }

            for (int i = 1; i < arguments.length; i++) {
                // XXX Simplify this!
                if (i % 2 == 1 && (!parameterSet.contains(arguments[i]) || parameters.containsKey(arguments[i]))) {
                    System.err.println("Error: Invalid or duplicated parameter '" + arguments[i] + "'");
                    return false;
                }
                if (i % 2 == 0) {
                    parameters.put(arguments[i - 1], arguments[i]);
                }
            }

            if (parameterSet.size() != parameters.size()) {
                System.err.println("Error: invalid commandline");
                return false;
            }

            if (arguments[0].equals(ACTION_SIGN)) {
                String privateKeyFileName = (String)parameters.get(PARAM_PRIVATEKEY);
                String inputFileName      = (String)parameters.get(PARAM_INPUTFILE);
                String outputFileName     = (String)parameters.get(PARAM_OUTPUTFILE);

                FileWriter w = null;
                try {
                    w = new FileWriter(outputFileName);
                    SignatureCreator creator = new SignatureCreator();
                    creator.signLicense(inputFileName, privateKeyFileName, w);
                } catch (Exception e) {
                    throw new SlmException("Error: failed creating output file: " + e.getMessage());
                } finally {
                    try { w.close(); }
                    catch (Exception e) {}
                }
            } else {
                String publicKeyFileName = (String)parameters.get(PARAM_PUBLICKEY);
                String inputFileName     = (String)parameters.get(PARAM_INPUTFILE);

                SignatureValidator validator = new SignatureValidator();
                String[] licenseLines = validator.verifyLicense(publicKeyFileName, inputFileName);
                if (licenseLines != null) {
                    System.out.println("License is valid.");
                    System.exit(0);
                } else {
                    System.out.println("License is NOT valid.");
                    System.exit(2);
                }
            }

            return true;
        } catch (SlmException ex) {
            System.err.println("Error during license validation: " + ex.getMessage());
            return false;
        }
    }
}
