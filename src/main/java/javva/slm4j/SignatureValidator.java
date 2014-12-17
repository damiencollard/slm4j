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

import java.io.*;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;

/** Validates provided signature file using DSA algorithm
 *
 * @author Gabor Toth
 */
public class SignatureValidator {

    private PublicKey publicKey;
    private Signature signature;
    private String token = "=";
    private StringBuffer licenseText = new StringBuffer();
    private byte[] licenseSignature;
    private HashMap licenseOptions = new HashMap();
    private static final String LICENSE_BEGIN = "----- BEGIN LICENSE -----";
    private static final String LICENSE_END = "----- END LICENSE -----";
    private static final String SIGNATURE_BEGIN = "----- BEGIN SIGNATURE -----";
    private static final String SIGNATURE_END = "----- END SIGNATURE -----";


    /**
     * Get the value of license options (signed key-value pairs, if presents)
     *
     * @return key-value pairs of strings
     */
    public HashMap getLicenseOptions() {
        return licenseOptions;
    }

    /**
     * Get the value of signed license contents
     *
     * @return the value of license contents
     */
    public String getLicenseText() {
        return licenseText.toString();
    }


    /**
     * Get the value of key-value separator token
     *
     * @return the value of key-value separator token
     */
    public String getToken() {
        return token;
    }

    /**
     * Set the value of the key-value separator token
     *
     * @param token new value of the token
     */
    public void setToken(String token) {
        this.token = token;
    }


    private void initializeSignatureVerify() throws SlmException {
        try {
            signature = Signature.getInstance("SHA1withDSA");
            signature.initVerify(publicKey);
        } catch (Exception ex) {
            throw new SlmException("Error in initializing signature for verification ( " + ex.getMessage() + " )");
        }
    }

    private void processSourceFile(String sourceFile) throws SlmException {
        FileReader fileReader = null;
        BufferedReader bufferedReader = null;
        try {
            fileReader = new FileReader(sourceFile);
            bufferedReader = new BufferedReader(fileReader);
            boolean isLicense = true;
            String line;
            int index = 0;

            isLicense = false;

            while (bufferedReader.ready() && !isLicense) {
                line = bufferedReader.readLine();
                if (line.equals(LICENSE_BEGIN)) {
                    isLicense = true;
                }
            }

            while (bufferedReader.ready() && isLicense) {
                line = bufferedReader.readLine();

                if (!line.equals(LICENSE_END)) {
                    signature.update(line.getBytes(), 0, line.getBytes().length);

                    licenseText.append(line);
                    licenseText.append(System.getProperty("line.separator"));
                    index = line.indexOf(token);
                    if ( index != -1 && index+1 <= line.length() )
                        licenseOptions.put(line.substring(0,index), line.substring(index+1));
                } else {
                    isLicense = false;
                }
            }

            licenseSignature = readSignature(bufferedReader);
        } catch (Exception ex) {
            throw new SlmException("Error in processing source file ( " + ex.getMessage() + " )");
        } finally {
            try {
                bufferedReader.close();
                fileReader.close();
            } catch (Exception ex) {
            }
        }
    }

    private boolean readPublicKey(String publicKeyFile) throws SlmException {
        FileReader fileReader = null;
        BufferedReader bufferedReader;

        try {
            if (!new File(publicKeyFile).exists()) {
                return false;
            }

            String publicKeyString = "";

            fileReader = new FileReader(publicKeyFile);
            bufferedReader = new BufferedReader(fileReader);

            while (bufferedReader.ready()) {
                publicKeyString += bufferedReader.readLine();
            }

            publicKey = KeyFactory.getInstance("DSA").generatePublic(new X509EncodedKeySpec(Base64Coder.decode(publicKeyString)));

            return true;
        } catch (Exception ex) {
            throw new SlmException("Error in reading public key from file " + publicKeyFile + " ( " + ex.getMessage() + " )");
        } finally {
            try {
                fileReader.close();
            } catch (Exception ex) {
            }
        }
    }

    private byte[] readSignature(BufferedReader bufferedReader) throws SlmException {
        try {
            boolean isSignature = false;
            String line;
            String signatureString = new String();

            while (bufferedReader.ready() && !isSignature) {
                line = bufferedReader.readLine();
                if (line.equals(SIGNATURE_BEGIN)) {
                    isSignature = true;
                }
            }

            while (bufferedReader.ready() && isSignature) {
                line = bufferedReader.readLine();
                if (line.equals(SIGNATURE_END)) {
                    isSignature = false;
                } else {
                    signatureString += line;
                }
            }

            return Base64Coder.decode(signatureString);
        } catch (Exception ex) {
            throw new SlmException("Error in reading signature from file ( " + ex.getMessage() + " )");
        }
    }

    private boolean verifySignature() throws SlmException {
        try {
            return signature.verify(licenseSignature);
        } catch (Exception ex) {
            throw new SlmException("Error in verification ( " + ex.getMessage() + " )");
        }
    }

    /** Verifies a license file based on public DSA key
     *
     * @param publicKeyFile Public key file for checking
     * @param signatureFile Signature file to check
     * @return true when the signature file matches, otherwise false
     * @throws SlmException Raised on IO or Crypthographic errors
     */
    public boolean verifyLicense(String publicKeyFile, String signatureFile) throws SlmException {
        try {
            readPublicKey(publicKeyFile);
            initializeSignatureVerify();
            processSourceFile(signatureFile);

            if (verifySignature()) {
                return true;
            } else {
                return false;
            }
        } catch (Exception ex) {
            throw new SlmException("Error in signature verification ( " + ex.getMessage() + " )");
        }
    }

    /** Verifies a license file based on public DSA key
     *
     * @param publicKeyString Public key content for checking
     * @param signatureFile Signature file to check
     * @return true when the signature file matches, otherwise false
     * @throws SlmException Raised on IO or Crypthographic errors
     */
    public boolean verifyLicenseWithString(String publicKeyString, String signatureFile) throws SlmException {
        try {
            publicKey = KeyFactory.getInstance("DSA").generatePublic(new X509EncodedKeySpec(Base64Coder.decode(publicKeyString)));

            initializeSignatureVerify();
            processSourceFile(signatureFile);

            if (verifySignature()) {
                return true;
            } else {
                return false;
            }
        } catch (Exception ex) {
            throw new SlmException("Error in signature verification ( " + ex.getMessage() + " )");
        }
    }
}
