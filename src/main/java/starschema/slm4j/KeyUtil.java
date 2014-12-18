package starschema.slm4j;

import java.io.FileWriter;
import java.io.Writer;
import java.security.*;

public class KeyUtil {
    // Length of a key line.
    static final int SIGNATURE_LINE_LENGTH = 20;

    public static void generateKeys(String privateKeyFileName, String publicKeyFileName) throws SlmException {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("DSA", "SUN");
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");

            gen.initialize(1024, random);

            KeyPair kp = gen.generateKeyPair();
            PrivateKey privateKey = kp.getPrivate();
            PublicKey  publicKey  = kp.getPublic();

            writeKey(privateKey, privateKeyFileName);
            writeKey(publicKey, publicKeyFileName);
        } catch (Exception e) {
            throw new SlmException("Error generating keys: " + e.getMessage());
        }
    }

    public static void writeKey(Key key, String fileName) throws SlmException {
        Writer w = null;
        try {
            w = new FileWriter(fileName);
            writeKey(key, w);
        } catch (Exception e) {
            throw new SlmException("Error writing key to " + fileName + ": " + e.getMessage());
        } finally {
            try { if (w != null) w.close(); }
            catch (Exception e) {}
        }
    }

    public static void writeKey(Key key, Writer w) throws SlmException {
        try {
            byte[] encodedKey = key.getEncoded();
            char[] keyString = Base64Coder.encode(encodedKey);
            writeKey(keyString, w);
        } catch (Exception e) {
            throw new SlmException("Error writing key: " + e.getMessage());
        }
    }

    public static void writeKey(char[] key, Writer w) throws SlmException {
        try {
            for (int i = 0; i < key.length; i = i + SIGNATURE_LINE_LENGTH) {
                w.write(key, i, Math.min(key.length - i, SIGNATURE_LINE_LENGTH));
                if (key.length - i > SIGNATURE_LINE_LENGTH) {
                    w.write(Util.EOL);
                }
            }
        } catch (Exception e) {
            throw new SlmException("Error writing key: " + e.getMessage());
        }
    }
}
