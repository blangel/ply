package net.ocheyedan.ply;

import net.ocheyedan.ply.exec.ClojureExec;
import net.ocheyedan.ply.exec.Execution;
import net.ocheyedan.ply.exec.JarExec;
import net.ocheyedan.ply.props.Prop;
import net.ocheyedan.ply.props.PropsExt;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * User: blangel
 * Date: 9/3/11
 * Time: 9:09 PM
 *
 * Executes build scripts.  The determination of where/which build script to execute is as follows:
 * -1- First resolve the script against the {@literal aliases} context properties in case it is an alias.
 * -2- For each resolved script (from -1-), check for an executable in the {@literal scripts.dir}
 * -3- If not there, check for an executable in the installation's scripts directory.
 * -4- If not there, check for executable directly (via the system path)
 * -5- else fail
 *
 * Note, any script or alias can be prefixed with a scope via 'scope_name:'.  This scope will determine the
 * set of resolved properties to pass to the execution.  If nothing is specified, it is the default scope and
 * so the default scoped properties are used (i.e., for context 'compiler' with default scope the properties are
 * resolved from the config dir from file 'compiler.properties').  If a scope is specified then that scope's properties
 * (inheriting from default if not overridden explicitly by the scope) are used (i.e., for context 'compiler' with
 * scope 'test' then the properties are resolved from the config dir from file 'compiler.test.properties' and if there
 * are any properties within the default, 'compiler.properties' which are not present in the scoped
 * 'compiler.test.properties' then they are also included).
 * For an alias with a scope then every script defined by the alias also has the scope.  For instance, say there exists
 * an alias 'install' which resolves to scripts 'file-changed compile package'.  If 'test:install' is invoked, in other
 * words the install alias is invoked with 'test' scope, then the resolved scripts to be invoked would be
 * 'test:file-changed test:compile test:package'.
 */
public final class Exec {

    /**
     * Invokes all scripts associated with {@code unresolved} by resolving it to a list of {@code Execution} objects
     * and then invoking them.
     * @param projectPlyDir the {@literal .ply} directory of the project to invoke
     * @param unresolved to resolve any aliases or script location
     * @return false if any of the invocations of the resolved {@link Execution} objects failed for any reason
     */
    public static boolean invoke(File projectPlyDir, String unresolved) {
        List<Execution> executions = resolveExecutions(unresolved, FileUtil.fromParts(projectPlyDir.getPath(), "config"));
        // all invoked scripts will be started from the parent of the '.ply' directory.
        // this provides a consistent view of execution for all scripts.  if a script wants to actually know
        // which directory from which the 'ply' command was invoked, look at 'original.user.dir' environment property.
        File projectRoot = FileUtil.fromParts(projectPlyDir.getPath(), "..");
        for (Execution execution : executions) {
            if (!invoke(execution, projectRoot)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param unresolved the alias/script to resolve into {@link Execution} objects.
     * @param projectConfigDir the ply configuration directory of the project for which to resolve properties
     * @return the list of {@link Execution} objects resolved by {@code unresolved}
     */
    private static List<Execution> resolveExecutions(String unresolved, File projectConfigDir) {
        return resolveExecutions(unresolved, null, "", new ArrayList<Execution>(), new ArrayList<String>(), projectConfigDir);
    }

    /**
     * The recursive variant of {@link #resolveExecutions(String, File)}
     * @param unresolved the alias/script to resolve into {@link Execution} objects.
     * @param propagatedOriginalScript the original script name before resolution from a previous recursive invocation
     * @param propagatedScope the scope from a previous recursive invocation
     * @param executions the list to add into for all {@link Execution} objects found from {@code unresolved}
     * @param encountered the list of scripts already encountered while resolving {@code unresolved} (used to detect
     *                    circular definitions of aliases).
     * @param projectConfigDir the ply configuration directory of the project for which to resolve properties
     * @return {@code executions} augmented with any resolved from {@code unresolved}
     */
    private static List<Execution> resolveExecutions(String unresolved, String propagatedOriginalScript,
                                                     String propagatedScope, List<Execution> executions,
                                                     List<String> encountered, File projectConfigDir) {
        String[] cmdArgs = splitScript(unresolved);
        String script = cmdArgs[0];
        String scope = propagatedScope;
        if (script.contains(":")) {
            int index = script.indexOf(":");
            scope = script.substring(0, index);
            if (index >= script.length() - 1) {
                Output.print("^error^ Scoped value [ ^b^%s^r^ ] must be followed by an alias or a script.", scope);
                System.exit(1);
            }
            cmdArgs[0] = script.substring(script.indexOf(":") + 1);
        } else if ((scope != null) && !scope.isEmpty()) {
            script = scope + ":" + script;
        }
        if (encountered.contains(script)) {
            Output.print("^error^ Alias (^b^%s^r^) contains a circular reference (run '^b^ply config --scripts get %s^r^' to analyze).",
                         script, script);
            System.exit(1);
        }
        encountered.add(script);
        resolveAlias(propagatedOriginalScript, script, cmdArgs, scope, executions, encountered, projectConfigDir);
        return executions;
    }

    /**
     * Looks up {@code args[0]} in the {@literal aliases} context of {@link Config} properties to see if it is an alias
     * for another command (or chain of commands).
     *
     * @param propagatedScript the previously encountered script (i.e., if 'install' -> 'compile' and 'compile' ->
     *                         file.jar compiler.jar, then the sequence of recursion for this variable would be:
     *                         null -> install -> compile (so that the end file.jar and compiler.jar's script name
     *                         will be the last alias value which is 'compile' (as that is more readable for the user).
     * @param originalScript the original unresolved script name
     * @param cmdArgs the script and arguments to it where {@code args[0]} is the script per convention of {@link Process}
     * @param scope to resolve the alias against
     * @param executions the list of resolved {@link Execution} objects.
     * @param encountered list of scripts already encountered while resolving {@code cmdArgs[0]}.
     * @param projectConfigDir the ply configuration directory of the project for which to resolve properties for resolving
     *                         aliases
     */
    private static void resolveAlias(String propagatedScript, String originalScript, String[] cmdArgs, String scope,
                                     List<Execution> executions, List<String> encountered, File projectConfigDir) {
        String script = cmdArgs[0];
        String namedScript = (propagatedScript == null ? originalScript : propagatedScript);
        Prop resolved = PropsExt.get(projectConfigDir, "aliases", scope, script);
        if (resolved == null) { // not an alias
            filter(cmdArgs, scope, projectConfigDir);
            executions.add(new Execution(namedScript, scope, cmdArgs[0], cmdArgs));
            return;
        }
        String scopeInfo = ((scope == null) || scope.isEmpty() ? "" : String.format(" (with scope ^b^%s^r^)", scope));
        Output.print("^info^ resolved ^b^%s^r^ to ^b^%s^r^%s", script, resolved.value, scopeInfo);
        String[] splitResolved = splitScript(resolved.value);
        for (String split : splitResolved) {
            int index = encountered.size() - 1; // mark position to pop after recursive call to resolveExecutions
            cmdArgs[0] = split;
            String unresolved = buildScriptName(cmdArgs); // TODO - how to propagate arguments? passed to each? passed to last?
            resolveExecutions(unresolved, originalScript, scope, executions, encountered, projectConfigDir);
            encountered = encountered.subList(0, index + 1); // treat like a stack, pop off
        }
    }

    /**
     * Invokes {@code execution} and routes all output to this process's output stream.
     * @param execution to invoke
     * @param projectRoot for which to set the root directory for the process handling the {@code execution}
     * @return false if the invocation of {@code execution} failed for any reason.
     */
    private static boolean invoke(Execution execution, File projectRoot) {
        File projectConfigDir = FileUtil.fromParts(projectRoot.getPath(), ".ply", "config");
        execution = resolveExecutable(execution, projectConfigDir);
        execution = handleNonNativeExecutable(execution, projectConfigDir);
        String script = buildScriptName(execution.scriptArgs); // TODO - only if need be; augment Output to have way of computing values only if the log level is valid
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(execution.scriptArgs).redirectErrorStream(true).directory(projectRoot);
            Map<String, String> environment = processBuilder.environment();
            environment.putAll(PropsExt.getPropsForEnv(FileUtil.fromParts(projectRoot.getPath(), ".ply"),
                                                       projectConfigDir,
                                                       execution.scope));
            Output.print("^dbug^ invoking %s", script);
            // the Process thread reaps the child if the parent (this) is terminated
            Process process = processBuilder.start();
            InputStream processStdout = process.getInputStream();
            BufferedReader lineReader = new BufferedReader(new InputStreamReader(processStdout));
            String processStdoutLine;
            while ((processStdoutLine = lineReader.readLine()) != null) {
                Output.printFromExec("[^green^%s^r^] %s", execution.originalScript, processStdoutLine);
            }
            int result = process.waitFor();
            if (result == 0) {
                return true;
            }
            Output.print("^error^ script ^green^%s^r^ failed [ exit code = %d ].", execution.originalScript, result);
        } catch (IOException ioe) {
            Output.print("^error^ executing script ^green^%s^r^", execution.originalScript);
            Output.print(ioe);
        } catch (InterruptedException ie) {
            Output.print(ie);
        }
        return false;
    }

    /**
     * Runs {@link PropsExt#filterForPly(java.io.File, net.ocheyedan.ply.props.Prop, String)} on each value
     * within {@code array}, the context will be scripts.
     * @param array to filter
     * @param scope of the {@code array} object's execution.
     * @param projectConfigDir the ply configuration directory of the project for which to resolve properties for filtering
     */
    private static void filter(String[] array, String scope, File projectConfigDir) {
        for (int i = 0; i < array.length; i++) {
            array[i] = PropsExt.filterForPly(projectConfigDir, new Prop("aliases", "", "", array[i], true), scope);
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

    private static Execution resolveExecutable(Execution execution, File projectConfigDir) {
        String passedInScript = execution.script;
        String script = execution.script;
        String localScriptsDir = PropsExt.filterForPly(projectConfigDir,
                PropsExt.get(projectConfigDir, "project", execution.scope, "scripts.dir"), execution.scope);
        script = (localScriptsDir.endsWith(File.separator) ? localScriptsDir :
                localScriptsDir + File.separator) + script;
        File scriptFile = new File(script);
        if (scriptFile.exists() && scriptFile.canExecute()) {
            return execution.with(script);
        } else if (scriptFile.exists()) {
            Output.print("^warn^ ^b^%s^r^ exists but is not executable, skipping.", scriptFile.getPath());
        }
        try {
            script = PlyUtil.SYSTEM_SCRIPTS_DIR.getCanonicalPath() + File.separator + passedInScript;
            scriptFile = new File(script);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        if (scriptFile.exists() && scriptFile.canExecute()) {
            return execution.with(script);
        } else if (scriptFile.exists()) {
            Output.print("^warn^ ^b^%s^r^ exists but is not executable, skipping.", scriptFile.getPath());
        }
        return execution;
    }

    /**
     * Translates {@code execution#scriptArgs} into an executable statement if it needs an invoker like a VM.
     * The whole command array needs to be processed as parameters to the VM may need to be inserted
     * into the command array.
     * @param execution to invoke
     * @param projectConfigDir the ply configuration directory from which to resolve properties
     * @return the translated execution.
     */
    private static Execution handleNonNativeExecutable(Execution execution, File projectConfigDir) {
        String script = execution.script;
        if (script.endsWith(".jar")) {
            return JarExec.createJarExecutable(execution, projectConfigDir);
        } else if (script.endsWith(".clj")) {
            return ClojureExec.createClojureExecutable(execution, projectConfigDir);
        }
        return execution;
    }

}