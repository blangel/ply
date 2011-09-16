package org.moxie.ply;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * User: blangel
 * Date: 9/3/11
 * Time: 9:09 PM
 *
 * Executes build scripts.  The determination of where/which build script to execute is as follows:
 * -1- First resolve the script via the {@link Config} properties in case it is aliased.
 * -2- For each resolved script (from -1-), check for an executable in the {@literal scripts.dir}
 * -3- If not there, check for an executable in the ply.scripts.dir.
 * -4- If not there, check for executable directly (via the path)
 * -4- else fail
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
        String originalScript = cmdArgs[0];
        List<String[]> resolvedCmds = resolve(originalScript, cmdArgs);
        for (String[] resolvedCmd : resolvedCmds) {
            if (!invoke(originalScript, resolvedCmd)) {
                return false;
            }
        }
        return true;
    }

    private static boolean invoke(String originalScriptName, String[] cmdArgs) {
        String color = COLOR_SWAP.get();
        cmdArgs[0] = resolveExecutable(cmdArgs[0]);
        cmdArgs = handleNonNativeExecutable(cmdArgs);
        String script = buildScriptName(cmdArgs);
        try {
            Output.print("^info^ invoking ^" + color + "^%s^r^", script);
            ProcessBuilder processBuilder = new ProcessBuilder(cmdArgs).redirectErrorStream(true);
            Map<String, String> environment = processBuilder.environment();
            Map<String, Config.Prop> properties = Config.getResolvedEnvironmentalProperties();
            for (String propKey : properties.keySet()) {
                Config.Prop prop = properties.get(propKey);
                environment.put(propKey, prop.value);
            }
            // the Process thread reaps the child if the parent is terminated
            Process process = processBuilder.start();
            InputStream processStdout = process.getInputStream();
            BufferedReader lineReader = new BufferedReader(new InputStreamReader(processStdout));
            String processStdoutLine;
            while ((processStdoutLine = lineReader.readLine()) != null) {
                Output.print("[^" + color + "^%s^r^] %s", originalScriptName, processStdoutLine);
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
        String prop = Config.get(command);
        if (prop != null) {
            Output.print("^info^ resolved ^b^%s^r^ to ^b^%s^r^", command, prop);
            String[] splitResolved = splitScript(prop);
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

    private static String resolveExecutable(String script) {
        String originalScript = script;
        String localScriptsDir = Config.get("scripts.dir");
        script = (localScriptsDir.endsWith(File.separator) ? localScriptsDir :
                localScriptsDir + File.separator) + script;
        File scriptFile = new File(script);
        if (scriptFile.exists() && scriptFile.canExecute()) {
            return script;
        } else if (scriptFile.exists()) {
            Output.print("^warn^ ^b^%s^r^ exists but is not executable, skipping.", scriptFile.getPath());
        }
        try {
            script = Config.GLOBAL_SCRIPTS_DIR.getCanonicalPath() + File.separator + originalScript;
            scriptFile = new File(script);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        if (scriptFile.exists() && scriptFile.canExecute()) {
            return script;
        } else if (scriptFile.exists()) {
            Output.print("^warn^ ^b^%s^r^ exists but is not executable, skipping.", scriptFile.getPath());
        }
        return originalScript;
    }

    /**
     * Translates {@code cmdArray[0]} into an executable statement if it needs an invoker like a VM.
     * The whole command array needs to be processed as parameters to the VM may need to be inserted
     * into the command array.
     * @param cmdArray to translate
     * @return the translated command array.
     */
    private static String[] handleNonNativeExecutable(String[] cmdArray) {
        String script = cmdArray[0];
        if (script.endsWith(".jar")) {
            // add the appropriate java command
            script = System.getProperty("ply.java");
            String[] newCmdArray = new String[cmdArray.length + 2];
            newCmdArray[0] = script;
            newCmdArray[1] = "-jar";
            for (int i = 0; i < cmdArray.length; i++) {
                newCmdArray[i + 2] = cmdArray[i];
            }
            cmdArray = newCmdArray;
        }
        return cmdArray;
    }

}
