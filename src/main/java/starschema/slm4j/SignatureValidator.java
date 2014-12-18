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

import java.security.*;
import java.security.spec.X509EncodedKeySpec;

public class SignatureValidator {
    private String[] extractLicense(String[] lines) throws SlmException {
        return Util.extractLines(lines, Delim.LICENSE_BEGIN, Delim.LICENSE_END);
    }

    private byte[] extractSignature(String[] lines, PublicKey _publicKey) throws SlmException {
        Signature sig;
        try {
            sig = Signature.getInstance("SHA1withDSA");
            sig.initVerify(_publicKey);
        } catch (Exception ex) {
            throw new SlmException("Error initializing signature: " + ex.getMessage());
        }

        String[] sigLines = Util.extractLines(lines, Delim.SIGNATURE_BEGIN, Delim.SIGNATURE_END);
        StringBuilder sb = new StringBuilder();
        for (String line: sigLines) {
            sb.append(line);
        }

        return Base64Coder.decode(sb.toString());
    }

    private boolean verifyTextSignature(String[] lines, byte[] sig, PublicKey _publicKey) throws SlmException {
        Signature computedSig;
        try {
            computedSig = Signature.getInstance("SHA1withDSA");
            computedSig.initVerify(_publicKey);
        } catch (Exception e) {
            throw new SlmException("Error initializing signature: " + e.getMessage());
        }

        try {
            for (String line: lines) {
                computedSig.update(line.getBytes(), 0, line.getBytes().length);
            }
            return computedSig.verify(sig);
        } catch (Exception e) {
            throw new SlmException("Error computing signature: " + e.getMessage());
        }
    }

    private PublicKey readPublicKey(String fileName) throws SlmException {
        try {
            String publicKeyString = Util.readFileContents(fileName, false);
            return KeyFactory.getInstance("DSA").generatePublic(
                    new X509EncodedKeySpec(Base64Coder.decode(publicKeyString)));
        } catch (Exception e) {
            throw new SlmException("Error reading public key file: " + e.getMessage());
        }
    }

    /** Verifies a signed license file against a public key.
     * Returns the license text lines if the license file is valid, null otherwise.
     */
    public String[] verifyLicense(String publicKeyFile, String signedFile) throws SlmException {
        try {
            PublicKey _publicKey = readPublicKey(publicKeyFile);

            String[] lines = Util.readLines(signedFile);
            String[] licenseLines = extractLicense(lines);
            byte[]   licenseSig   = extractSignature(lines, _publicKey);

            if (verifyTextSignature(licenseLines, licenseSig, _publicKey))
                return licenseLines;
            else
                return null;
        } catch (Exception e) {
            throw new SlmException("Error in signature verification: " + e.getMessage());
        }
    }
}
