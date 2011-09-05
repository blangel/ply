package org.moxie.ply;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
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
        String[] cmdArgs = splitScript(script);
        List<String[]> resolvedCmds = resolve(cmdArgs[0], cmdArgs);
        for (String[] resolvedCmd : resolvedCmds) {
            if (!invoke(resolvedCmd)) {
                return false;
            }
        }
        return true;
    }

    private static boolean invoke(String[] cmdArgs) {
        String color = COLOR_SWAP.get();
        String script = buildScriptName(cmdArgs);
        try {
            Output.print("^info^ invoking ^" + color + "^%s^r^", script);
            Process process = new ProcessBuilder(cmdArgs).redirectErrorStream(true).start();
            InputStream processStdout = process.getInputStream();
            BufferedReader lineReader = new BufferedReader(new InputStreamReader(processStdout));
            String processStdoutLine;
            while ((processStdoutLine = lineReader.readLine()) != null) {
                Output.print("[^" + color + "^%s^r^] %s", script, processStdoutLine);
            }
            int result = process.waitFor();
            if (result == 0) {
                return true;
            }
            Output.print("^error^ script ^" + color + "^%s^r^ failed [ result = %d ].", script, result);
        } catch (IOException ioe) {
            Output.print("^error^ executing script ^" + color + "^%s^r^", script);
            Output.print(ioe);
        } catch (InterruptedException ie) {
            Output.print(ie);
        }
        return false;
    }

    /**
     * Looks up {@code command} in the {@link Config} properties to see if it is an alias for another
     * command (or chain of commands).
     *
     * @param command to resolve
     * @param cmdArgs the arguments to {@code command} where {@code cmdArgs[0] == command} per convention of {@link Process}
     * @return the list of resolved commands (as {@code command} could be aliased as multiple commands) where
     *         each command's arguments have been filtered ({@literal ${xx}} is replaced by property named {@literal xx}
     *         from {@link Config#get(String)}).
     */
    private static List<String[]> resolve(String command, String[] cmdArgs) {
        List<String[]> resolved = new ArrayList<String[]>();
        Config.Prop prop = Config.get(command);
        if (prop != null) {
            Output.print("^info^ resolved ^b^%s^r^ to ^b^%s^r^", command, prop.value);
            String[] splitResolved = splitScript(prop.value);
            // TODO - recur resolve(String) on resolved
            for (String split : splitResolved) {
                String[] args = splitScript(split);
                String[] combined = combine(args, 0, args.length, cmdArgs, 1, cmdArgs.length - 1);
                filter(combined);
                resolved.add(combined);
            }
        } else {
            filter(cmdArgs);
            resolved.add(cmdArgs);
        }
        return resolved;
    }

    private static void filter(String[] array) {
        for (int i = 0; i < array.length; i++) {
            array[i] = Config.filter(array[i]);
        }
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

    private static String[] combine(String[] first, int firstStart, int firstLength, String[] last, int lastStart, int lastLength) {
        String[] combined = new String[firstLength + lastLength];
        int index = 0;
        for (int i = firstStart; i < (firstStart + firstLength); i++) {
            combined[index++] = first[i];
        }
        for (int i = lastStart; i < (lastStart + lastLength); i++) {
            combined[index++] = last[i];
        }
        return combined;
    }

    private static String buildScriptName(String[] cmdArgs) {
        StringBuilder buffer = new StringBuilder();
        for (String cmdArg : cmdArgs) {
            buffer.append(cmdArg);
            buffer.append(" ");
        }
        buffer.replace(buffer.length() - 1, buffer.length(), "");
        return buffer.toString();
    }

}
