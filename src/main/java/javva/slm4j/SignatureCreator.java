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
import java.security.spec.PKCS8EncodedKeySpec;

/** Signs an input file with DSA algorythm
 *
 * @author Gabor Toth
 */
public class SignatureCreator {

    private PublicKey publicKey;
    private PrivateKey privateKey;
    private Signature signature;
    private String licenseText;
    private static final String EOL = System.getProperty("line.separator");
    private static final String LICENSE_BEGIN = "----- BEGIN LICENSE -----";
    private static final String LICENSE_END = "----- END LICENSE -----";
    private static final String SIGNATURE_BEGIN = "----- BEGIN SIGNATURE -----";
    private static final String SIGNATURE_END = "----- END SIGNATURE -----";
    private static final int SIGNATURE_LINE_LENGTH = 20;

    private void generateKeys() throws SlmException {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA", "SUN");
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");

            keyGen.initialize(1024, random);
            KeyPair pair = keyGen.generateKeyPair();
            privateKey = pair.getPrivate();
            publicKey = pair.getPublic();
        } catch (Exception ex) {
            throw new SlmException("Error in key generation ( " + ex.getMessage() + " )");
        }
    }

    private void initializeSignatureSign() throws SlmException {
        try {
            signature = Signature.getInstance("SHA1withDSA", "SUN");
            signature.initSign(privateKey);
        } catch (Exception ex) {
            throw new SlmException("Error in initializing signature for signing ( " + ex.getMessage() + " )");
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

            licenseText = new String();

            while (bufferedReader.ready() && isLicense) {
                line = bufferedReader.readLine();
                if (!line.equals(LICENSE_END)) {
                    licenseText += line + EOL;
                    signature.update(line.getBytes(), 0, line.getBytes().length);
                } else {
                    isLicense = false;
                }
            }

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

    private char[] generateSignature() throws SlmException {
        try {
            return Base64Coder.encode(signature.sign());
        } catch (Exception ex) {
            throw new SlmException("Error in signature creation ( " + ex.getMessage() + " )");
        }
    }

    private void writeSignature(String signatureFile) throws SlmException {
        FileWriter fileWriter = null;

        try {
            fileWriter = new FileWriter(signatureFile);
            char[] signatureString = generateSignature();

            fileWriter.write(LICENSE_BEGIN);
            fileWriter.write(EOL + licenseText);
            fileWriter.write(EOL + LICENSE_END);

            fileWriter.write(EOL + SIGNATURE_BEGIN + EOL);

            for (int i = 0; i < signatureString.length; i = i + SIGNATURE_LINE_LENGTH) {
                fileWriter.write(signatureString, i, Math.min(signatureString.length - i, SIGNATURE_LINE_LENGTH));
                if (signatureString.length - i > SIGNATURE_LINE_LENGTH) {
                    fileWriter.write(EOL);
                }
            }

            fileWriter.write(EOL + SIGNATURE_END);
        } catch (Exception ex) {
            throw new SlmException("Error in writing signature to file " + signatureFile + " ( " + ex.getMessage() + " )");
        } finally {
            try {
                fileWriter.close();
            } catch (Exception ex) {
            }
        }
    }

    private void writePrivateKey(String privateKeyFile) throws SlmException {
        FileWriter fileWriter = null;

        try {
            byte[] encodedPrivateKey = privateKey.getEncoded();
            fileWriter = new FileWriter(privateKeyFile);
            char[] privateKeyString = Base64Coder.encode(encodedPrivateKey);

            for (int i = 0; i < privateKeyString.length; i = i + SIGNATURE_LINE_LENGTH) {
                fileWriter.write(privateKeyString, i, Math.min(privateKeyString.length - i, SIGNATURE_LINE_LENGTH));
                if (privateKeyString.length - i > SIGNATURE_LINE_LENGTH) {
                    fileWriter.write(EOL);
                }
            }
        } catch (Exception ex) {
            throw new SlmException("Error in writing private key to file " + privateKeyFile + " ( " + ex.getMessage() + " )");
        } finally {
            try {
                fileWriter.close();
            } catch (Exception ex) {
            }
        }
    }

    private void writePublicKey(String publicKeyFile) throws SlmException {
        FileWriter fileWriter = null;

        try {
            byte[] encodedPublicKey = publicKey.getEncoded();
            fileWriter = new FileWriter(publicKeyFile);
            char[] publicKeyString = Base64Coder.encode(encodedPublicKey);

            for (int i = 0; i < publicKeyString.length; i = i + SIGNATURE_LINE_LENGTH) {
                fileWriter.write(publicKeyString, i, Math.min(publicKeyString.length - i, SIGNATURE_LINE_LENGTH));
                if (publicKeyString.length - i > SIGNATURE_LINE_LENGTH) {
                    fileWriter.write(EOL);
                }
            }
        } catch (Exception ex) {
            throw new SlmException("Error in writing public key to file " + publicKeyFile + " ( " + ex.getMessage() + " )");
        } finally {
            try {
                fileWriter.close();
            } catch (Exception ex) {
            }
        }
    }

    private boolean readPrivateKey(String privateKeyFile) throws SlmException {
        FileReader fileReader = null;
        BufferedReader bufferedReader;

        try {
            if (!new File(privateKeyFile).exists()) {
                return false;
            }

            String privateKeyString = "";

            fileReader = new FileReader(privateKeyFile);
            bufferedReader = new BufferedReader(fileReader);

            while (bufferedReader.ready()) {
                privateKeyString += bufferedReader.readLine();
            }

            privateKey = KeyFactory.getInstance("DSA", "SUN").generatePrivate(new PKCS8EncodedKeySpec(Base64Coder.decode(privateKeyString)));

            return true;
        } catch (Exception ex) {
            throw new SlmException("Error in reading private key from file " + privateKeyFile + " ( " + ex.getMessage() + " )");
        } finally {
            try {
                fileReader.close();
            } catch (Exception ex) {
            }
        }
    }


    /** Signs an input file by the provided DSA keys
     *
     * If the private key does not exists, then a new one will be generated
     *
     * @param licenseFile Input file to sign
     * @param publicKeyFile Location of the previously generated public key file
     * @param privateKeyFile Location of the previously generated private key file
     * @param signatureFile Signed, output file (eg. user's license)
     * @throws SlmException On any error (File IO problems and CryptoAPI errors)
     */
    public void signLicense(String licenseFile, String publicKeyFile, String privateKeyFile, String signatureFile) throws SlmException {
        try {
            if (!readPrivateKey(privateKeyFile)) {
                generateKeys();
            }
            initializeSignatureSign();
            processSourceFile(licenseFile);
            writeSignature(signatureFile);
            if (!readPrivateKey(privateKeyFile)) {
                writePublicKey(publicKeyFile);
                writePrivateKey(privateKeyFile);
            }
        } catch (Exception ex) {
            throw new SlmException("Error in signature generation ( " + ex.getMessage() + " )");
        }
    }
}
