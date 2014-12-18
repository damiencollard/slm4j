package starschema.slm4j;

import java.io.Writer;

public class KeyUtil {
    // Length of a key line.
    static final int SIGNATURE_LINE_LENGTH = 20;

    public static void writeKey(char[] key, Writer w) throws SlmException {
        try {
            for (int i = 0; i < key.length; i = i + SIGNATURE_LINE_LENGTH) {
                w.write(key, i, Math.min(key.length - i, SIGNATURE_LINE_LENGTH));
                if (key.length - i > SIGNATURE_LINE_LENGTH) {
                    w.write(Util.EOL);
                }
            }
        } catch (Exception e) {
            throw new SlmException("Error writing key");
        }
    }
}
