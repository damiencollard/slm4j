package starschema.slm4j;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

public class Util {
    // The newline string.
    private static final String EOL = System.getProperty("line.separator");

    /** Returns the lines of a file. */
    public static String[] readLines(String fileName) throws SlmException {
        FileReader fileReader = null;
        BufferedReader bufferedReader = null;
        ArrayList<String> al = new ArrayList<>();
        try {
            fileReader = new FileReader(fileName);
            bufferedReader = new BufferedReader(fileReader);
            String line;
            while (bufferedReader.ready()) {
                line = bufferedReader.readLine();
                al.add(line);
            }
        } catch (Exception ex) {
            throw new SlmException("Error reading file: " + ex.getMessage());
        } finally {
            try {
                if (bufferedReader != null)
                    bufferedReader.close();
                if (fileReader != null)
                    fileReader.close();
            } catch (Exception ex) {
            }
        }

        return al.toArray(new String[al.size()]);
    }

    /** Returns the contents of a file as a string.
     *
     * If keepLines is true, the newlines aren't included in the returned string.
     * Otherwise, they are.
     */
    public static String readFileContents(String fileName, boolean keepLines) throws SlmException {
        String[] lines = readLines(fileName);
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line);
            if (keepLines)
                sb.append(EOL);
        }

        return sb.toString();
    }

    /** Returns the lines between the specified delimiters. */
    public static String[] extractLines(String[] lines, String beginDelim, String endDelim) {
        ArrayList<String> al = new ArrayList<>();
        boolean inSection = false;
        for (String line: lines) {
            if (line.equals(beginDelim)) {
                inSection = true;
            } else if (line.equals(endDelim)) {
                break;
            } else {
                if (inSection)
                    al.add(line);
            }
        }
        return al.toArray(new String[al.size()]);
    }
}
