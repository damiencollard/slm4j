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

/**
 * Starschema License Manager 4 Java - an easy-to-use, simple license file
 * generator and validator engine
 */
package starschema.slm4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/** Main class for slm4j command line tool
 *
 * @author Gabor Toth
 */
public class Main {

    private static final String OPTION_SIGN = "sign";
    private static final String OPTION_VERIFY = "verify";
    private static final String PARAMETER_LICENSE = "-license";
    private static final String PARAMETER_PUBLIC = "-public";
    private static final String PARAMETER_PRIVATE = "-private";
    private static final String PARAMETER_SIGNATURE = "-sign";

    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        try {
            executeApplication(args);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println(ex.getMessage());
        }
    }

    /** Main entry function for slm4j command line tool
     *<p>
     * Usage: java -jar SignatureCreator.jar <action> [parameters]
     *<p>
     *   Actions:
     *     sign                         Write a signature in in the signature file after
     *                                  the content of the source file.
     *     verify                       Verifies a signature file based on the key file.
     *   Parameters:
     *     -license license_file        Source file to add signature.
     *     -public public_key_file      Public key file. If sign then the public key will
     *                                  be written in this file. If verify then the verification
     *                                  will based on the public key stored in this file.
     *     -private private_key_file    Private key file. Available only for sign. The private
     *                                  key will be stored in this file.
     *     -sign signature_file         The signed license file. It is the output file of the sign
     *                                  and the input of the verification.
     * @param arguments Command line arguments
     * @return true on success, otherwise false
     */
    public static boolean executeApplication(String[] arguments) {
        HashMap parameters = new HashMap();
        Set parameterSet;
        Set parameterSetSign = new HashSet();
        Set parameterSetVerify = new HashSet();
        String usageString[];

        try {
            parameterSetSign.add(PARAMETER_LICENSE);
            parameterSetSign.add(PARAMETER_PUBLIC);
            parameterSetSign.add(PARAMETER_PRIVATE);
            parameterSetSign.add(PARAMETER_SIGNATURE);

            parameterSetVerify.add(PARAMETER_PUBLIC);
            parameterSetVerify.add(PARAMETER_SIGNATURE);

            usageString = new String[9];

            usageString[0] = "Usage: java -jar SignatureCreator.jar <action> [parameters]";
            usageString[1] = "\nActions:";
            usageString[2] = "  sign                         Write a signature in in the signature file after the content of the source file.";
            usageString[3] = "  verify                       Verifies a signature file based on the key file.";
            usageString[4] = "\nParameters:";
            usageString[5] = "  -license license_file        Source file to add signature.";
            usageString[6] = "  -public public_key_file      Public key file. If sign then the public key will be written in this file. If verify then the verification will based on the public key stored in this file.";
            usageString[7] = "  -private private_key_file    Private key file. Available only for sign. The private key will be stored in this file.";
            usageString[8] = "  -sign signature_file         The signed license file. It is the output file of the sign and the input of the verification.";

            if (arguments.length == 0) {
                for( int i = 0; i < usageString.length ; i++ )
                    System.out.println( usageString[i]);
                return false;
            }

            if (arguments.length % 2 != 1) {
                System.out.println("Wrong number of arguments");
                return false;
            }

            if (arguments[0].toLowerCase().equals(OPTION_SIGN)) {
                parameterSet = parameterSetSign;
            } else if (arguments[0].toLowerCase().equals(OPTION_VERIFY)) {
                parameterSet = parameterSetVerify;
            } else {
                System.out.println("Invalid option - Choose \"sign\", \"verify\"");
                return false;
            }

            for (int i = 1; i < arguments.length; i++) {
                if (i % 2 == 1 && (!parameterSet.contains(arguments[i]) || parameters.containsKey(arguments[i]))) {
                    System.out.println("Invalid or duplicated parameter \"" + arguments[i] + "\"");
                    return false;
                }
                if (i % 2 == 0) {
                    parameters.put(arguments[i - 1], arguments[i]);
                }
            }

            if (parameterSet.size() != parameters.size()) {
                System.out.println("All of the parameters must be set " + parameterSet);
                return false;
            }

            if (arguments[0].equals(OPTION_SIGN)) {
                new SignatureCreator().signLicense((String) parameters.get(PARAMETER_LICENSE), (String) parameters.get(PARAMETER_PUBLIC), (String) parameters.get(PARAMETER_PRIVATE), (String) parameters.get(PARAMETER_SIGNATURE));
            } else {
                SignatureValidator validator = new SignatureValidator();
                if (validator.verifyLicense((String) parameters.get(PARAMETER_PUBLIC), (String) parameters.get(PARAMETER_SIGNATURE))) {
                    System.out.println("License is valid");
                } else {
                    System.out.println("License is not valid");                    
                }
            }

            return true;
        } catch (SlmException ex) {
            System.err.println("Error during license validation: " + ex.getMessage());
            return false;
        }
    }
}
