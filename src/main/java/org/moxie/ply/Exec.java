package org.moxie.ply;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: blangel
 * Date: 9/3/11
 * Time: 9:09 PM
 *
 * Executes build scripts.  The determination of where/which build script to execute is as follows:
 * -1- check for an executable in the current directory.
 * -2- if not found, check the scripts directory under the install directory
 * -3- else fail
 */
public final class Exec {

    /**
     * Swap between colors when displaying script names (so that chained script invocations' output is
     * easily distinguishable).
     */
    private static class ColorSwap {
        private boolean swap = false;
        private synchronized String get() {
            String color = swap ? "green" : "magenta";
            swap = !swap;
            return color;
        }
    }
    private static final ColorSwap COLOR_SWAP = new ColorSwap();

    public static boolean invoke(String script) {
        String color = COLOR_SWAP.get();
        try {
            Process process = new ProcessBuilder(splitScript(script)).redirectErrorStream(true).start();
            InputStream processStdout = process.getInputStream();
            BufferedReader lineReader = new BufferedReader(new InputStreamReader(processStdout));
            String processStdoutLine;
            while ((processStdoutLine = lineReader.readLine()) != null) {
                Output.print("^ply^ [^" + color + "^%s^r^] %s", script, processStdoutLine);
            }
            int result = process.waitFor();
            if (result == 0) {
                return true;
            }
            Output.print("^error^ script ^" + color + "^%s^r^ failed.", script);
        } catch (IOException ioe) {
            Output.print("^error^ executing script ^" + color + "^%s^r^", script);
            Output.print(ioe);
        } catch (InterruptedException ie) {
            Output.print(ie);
        }
        return false;
    }

    /**
     * The {@link ProcessBuilder} expects the command to be in the form String[] { prog, arg1, arg2, ..., argn}
     * where the first entry in the array is the program name and subsequent entries are arguments to the program.
     * @param script to split
     * @return the command array
     */
    private static String[] splitScript(String script) {
        List<String> matchList = new ArrayList<String>();
        StringBuilder buffer = new StringBuilder();
        boolean withinQuotations = false;
        char[] characters = script.toCharArray();
        // split by spaces, ignoring spaces within quotation marks
        for (int i = 0; i < characters.length; i++) {
            char cur = characters[i];
            if ((' ' == cur) && !withinQuotations) {
                matchList.add(buffer.toString());
                buffer = new StringBuilder();
            } else if (('"' == cur)
                    && ((buffer.length() == 0) || ((i == (characters.length - 1)) || (' ' == characters[i + 1])))) {
                withinQuotations = (buffer.length() == 0);
            } else {
                buffer.append(cur);
            }
        }
        matchList.add(buffer.toString());
        return matchList.toArray(new String[matchList.size()]);
    }

}
