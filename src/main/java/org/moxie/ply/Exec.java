package org.moxie.ply;

import java.io.*;
import java.util.*;

/**
 * User: blangel
 * Date: 9/3/11
 * Time: 9:09 PM
 *
 * Executes build scripts.  The determination of where/which build script to execute is as follows:
 * -1- First resolve the script via the {@literal scripts} context {@link Config} properties in case it is aliased.
 * -2- For each resolved script (from -1-), check for an executable in the {@literal scripts.dir}
 * -3- If not there, check for an executable in the installation's scripts directory.
 * -4- If not there, check for executable directly (via the system path)
 * -5- else fail
 */
public final class Exec {

    public static boolean invoke(String script) {
        String[] cmdArgs = splitScript(script);
        String originalScript = cmdArgs[0];
        List<String[]> resolvedCmds = new ArrayList<String[]>();
        Set<String> encountered = new HashSet<String>();
        resolveAlias(cmdArgs, resolvedCmds, encountered);
        // all invoked scripts will be started from the parent of the '.ply' directory.
        // this provides a consistent view of execution for all scripts.  if a script wants to actually know
        // which directory from which the 'ply' command was invoked, look at 'parent.user.dir' environment property.
        String plyDirPath = Config.LOCAL_PROJECT_DIR.getPath();
        File projectRoot = new File(plyDirPath + (plyDirPath.endsWith(File.separator) ? "" : File.separator) + ".." + File.separator);
        for (String[] resolvedCmd : resolvedCmds) {
            if (!invoke(originalScript, resolvedCmd, projectRoot)) {
                return false;
            }
        }
        return true;
    }

    private static boolean invoke(String originalScriptName, String[] cmdArgs, File projectRoot) {
        String scriptWithoutPath = cmdArgs[0];
        cmdArgs[0] = resolveExecutable(cmdArgs[0]);
        cmdArgs = handleNonNativeExecutable(scriptWithoutPath, cmdArgs);
        String script = buildScriptName(cmdArgs);
        try {
            Output.print("^dbug^ invoking %s", script);
            ProcessBuilder processBuilder = new ProcessBuilder(cmdArgs).redirectErrorStream(true).directory(projectRoot);
            Map<String, String> environment = processBuilder.environment();
            Map<String, Config.Prop> properties = Config.getResolvedEnvironmentalProperties();
            for (String propKey : properties.keySet()) {
                Config.Prop prop = properties.get(propKey);
                environment.put(propKey, Config.filter(prop));
            }
            // the Process thread reaps the child if the parent (this) is terminated
            Process process = processBuilder.start();
            InputStream processStdout = process.getInputStream();
            BufferedReader lineReader = new BufferedReader(new InputStreamReader(processStdout));
            String processStdoutLine;
            while ((processStdoutLine = lineReader.readLine()) != null) {
                Output.print("[^green^%s^r^] %s", originalScriptName, processStdoutLine);
            }
            int result = process.waitFor();
            if (result == 0) {
                return true;
            }
            Output.print("^error^ script ^green^%s^r^ failed [ result = %d ].", originalScriptName, result);
        } catch (IOException ioe) {
            Output.print("^error^ executing script ^green^%s^r^", originalScriptName);
            Output.print(ioe);
        } catch (InterruptedException ie) {
            Output.print(ie);
        }
        return false;
    }

    /**
     * Looks up {@code args[0]} in the {@literal scripts} context of {@link Config} properties to see if it is an alias
     * for another command (or chain of commands).
     *
     * @param args the script and arguments to it where {@code args[0]} is the script per convention of {@link Process}
     * @param resolvedArgs the list of resolved scripts (as {@code args[0]} could be aliased as multiple commands) where
     *         each command's arguments have been filtered ({@literal ${xx}} is replaced by property named {@literal xx}
     *         from {@link Config#get(String)}).
     * @param encountered set of scripts already encountered while trying to resolve aliases.  Used to keep track
     *         of possible circular references.
     */
    private static void resolveAlias(String[] args, List<String[]> resolvedArgs, Set<String> encountered) {
        String script = args[0];
        if (encountered.contains(script)) {
            Output.print("^error^ script contains a circular reference to another script [ %s ].", script);
            System.exit(1);
        }
        encountered.add(script);
        String resolved = Config.get("scripts", script);
        if (resolved == null) {
            filter(args);
            resolvedArgs.add(args);
            return;
        }
        Output.print("^info^ resolved ^b^%s^r^ to ^b^%s^r^", script, resolved);
        String[] splitResolved = splitScript(resolved);
        for (String split : splitResolved) {
            String[] splitArgs = splitScript(split);
            String[] combined = combine(splitArgs, 0, splitArgs.length, args, 1, args.length - 1);
            resolveAlias(combined, resolvedArgs, encountered);
        }
    }

    /**
     * Runs {@link Config#filter(Config.Prop)} on each value within {@code array}, the context will be scripts.
     * @param array to filter
     */
    private static void filter(String[] array) {
        for (int i = 0; i < array.length; i++) {
            array[i] = Config.filter(new Config.Prop("scripts", "", array[i], true));
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
        String localScriptsDir = Config.get("project", "scripts.dir");
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
     * @param unresolvedScript the unresolved script name (i.e., with path information).
     * @param cmdArray to translate
     * @return the translated command array.
     */
    private static String[] handleNonNativeExecutable(String unresolvedScript, String[] cmdArray) {
        String script = cmdArray[0];
        if (script.endsWith(".jar")) {
            cmdArray = JarExec.createJarExecutable(unresolvedScript, cmdArray);
        }
        return cmdArray;
    }

}
