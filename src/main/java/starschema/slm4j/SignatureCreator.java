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

public class SignatureCreator {
    private Signature computeTextSignature(String[] lines, PrivateKey _privateKey) throws SlmException {
        Signature _signature;
        try {
            _signature = Signature.getInstance("SHA1withDSA", "SUN");
            _signature.initSign(_privateKey);
        } catch (Exception e) {
            throw new SlmException("Error initializing signature: " + e.getMessage());
        }

        boolean inLicense = true;
        try {
            for (String line: lines) {
                inLicense = !line.equals(Delim.LICENSE_END);
                if (!inLicense)
                    break;
                _signature.update(line.getBytes(), 0, line.getBytes().length);
            }
            return _signature;
        } catch (Exception e) {
            throw new SlmException("Error processing source file: " + e.getMessage());
        }
    }

    private PrivateKey readPrivateKey(String fileName) throws SlmException {
        try {
            String privateKeyString = Util.readFileContents(fileName, false);
            return KeyFactory.getInstance("DSA", "SUN").generatePrivate(
                    new PKCS8EncodedKeySpec(Base64Coder.decode(privateKeyString)));
        } catch (Exception e) {
            throw new SlmException("Error reading private key file: " + e.getMessage());
        }
    }

    public void signLicense(String licenseFileName, String privateKeyFile, Writer w) throws SlmException {
        try {
            String[] lines = Util.readLines(licenseFileName);
            PrivateKey _privateKey = readPrivateKey(privateKeyFile);

            Signature sig = computeTextSignature(lines, _privateKey);

            // Generate the Base64-encoded signature.
            char[] base64Sig = null;
            try {
                base64Sig = Base64Coder.encode(sig.sign());
            } catch (Exception ex) {
                throw new SlmException("Error computing the signature: " + ex.getMessage());
            }

            w.write(Delim.LICENSE_BEGIN);
            w.write(Util.EOL);
            for (String line: lines) {
                w.write(line);
                w.write(Util.EOL);
            }
            w.write(Delim.LICENSE_END);
            w.write(Util.EOL);

            w.write(Delim.SIGNATURE_BEGIN); w.write(Util.EOL);
            for (int i = 0; i < base64Sig.length; i = i + KeyUtil.SIGNATURE_LINE_LENGTH) {
                w.write(base64Sig, i, Math.min(base64Sig.length - i, KeyUtil.SIGNATURE_LINE_LENGTH));
                if (base64Sig.length - i > KeyUtil.SIGNATURE_LINE_LENGTH) {
                    w.write(Util.EOL);
                }
            }
            w.write(Util.EOL);
            w.write(Delim.SIGNATURE_END);
        } catch (Exception ex) {
            throw new SlmException("Error signing file: " + ex.getMessage());
        }
    }
}
