package org.moxie.ply;

import org.moxie.ply.props.Prop;
import org.moxie.ply.props.Props;

import javax.print.DocFlavor;
import java.io.*;
import java.util.*;

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
     * A struct of execution information.
     */
    static final class Execution {

        /**
         * The original, unresolved, script/alias.
         */
        final String originalScript;

        /**
         * The scope of this execution.
         */
        final String scope;

        /**
         * The resolved script.
         */
        final String script;

        /**
         * The arguments to the execution for the script.  By convention of {@link Process} the
         * first item is the {@link #script} itself.  So for an execution without args this
         * array has one value, the script to execute.
         */
        final String[] scriptArgs;

        Execution(String originalScript, String scope, String script, String[] scriptArgs) {
            this.originalScript = originalScript;
            this.scope = scope;
            this.script = script;
            this.scriptArgs = scriptArgs;
        }

        Execution with(String script) {
            String[] args = new String[this.scriptArgs.length];
            System.arraycopy(this.scriptArgs, 1, args, 1, this.scriptArgs.length - 1);
            args[0] = script;
            return new Execution(this.originalScript, this.scope, script, args);
        }

        Execution with(String[] args) {
            return new Execution(this.originalScript, this.scope, args[0], args);
        }

        @Override public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Execution execution = (Execution) o;
            if (scope != null ? !scope.equals(execution.scope) : execution.scope != null) {
                return false;
            }
            return (script == null ? (execution.script == null) : script.equals(execution.script));
        }

        @Override public int hashCode() {
            int result = scope != null ? scope.hashCode() : 0;
            result = 31 * result + (script != null ? script.hashCode() : 0);
            return result;
        }
    }

    /**
     * Invokes all scripts associated with {@code unresolved} by resolving it to a list of {@code Execution} objects
     * and then invoking them.
     * @param unresolved to resolve any aliases or script location
     * @return false if any of the invocations of the resolved {@link Execution} objects failed for any reason
     */
    public static boolean invoke(String unresolved) {
        List<Execution> executions = resolveExecutions(unresolved);
        // all invoked scripts will be started from the parent of the '.ply' directory.
        // this provides a consistent view of execution for all scripts.  if a script wants to actually know
        // which directory from which the 'ply' command was invoked, look at 'parent.user.dir' environment property.
        String plyDirPath = PlyUtil.LOCAL_PROJECT_DIR.getPath();
        File projectRoot = new File(plyDirPath + (plyDirPath.endsWith(File.separator) ? "" : File.separator) + ".." + File.separator);
        for (Execution execution : executions) {
            if (!invoke(execution, projectRoot)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param unresolved the alias/script to resolve into {@link Execution} objects.
     * @return the list of {@link Execution} objects resolved by {@code unresolved}
     */
    private static List<Execution> resolveExecutions(String unresolved) {
        return resolveExecutions(unresolved, null, "", new ArrayList<Execution>(), new ArrayList<String>());
    }

    /**
     * The recursive variant of {@link #resolveExecutions(String)}
     * @param unresolved the alias/script to resolve into {@link Execution} objects.
     * @param propagatedOriginalScript the original script name before resolution from a previous recursive invocation
     * @param propagatedScope the scope from a previous recursive invocation
     * @param executions the list to add into for all {@link Execution} objects found from {@code unresolved}
     * @param encountered the list of scripts already encountered while resolving {@code unresolved} (used to detect
     *                    circular definitions of aliases).
     * @return {@code executions} augmented with any resolved from {@code unresolved}
     */
    private static List<Execution> resolveExecutions(String unresolved, String propagatedOriginalScript,
                                                     String propagatedScope, List<Execution> executions,
                                                     List<String> encountered) {
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
        resolveAlias(propagatedOriginalScript, script, cmdArgs, scope, executions, encountered);
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
     */
    private static void resolveAlias(String propagatedScript, String originalScript, String[] cmdArgs, String scope, List<Execution> executions,
                                     List<String> encountered) {
        String script = cmdArgs[0];
        String namedScript = (propagatedScript == null ? originalScript : propagatedScript);
        Prop resolved = Props.get("aliases", scope, script);
        if (resolved == null) { // not an alias
            filter(cmdArgs, scope);
            executions.add(new Execution(namedScript, scope, cmdArgs[0], cmdArgs));
            return;
        }
        String scopeInfo = ((scope == null) || scope.isEmpty() ? "" : String.format(" (with scope ^b^%s^r^)", scope));
        Output.print("^info^ resolved ^b^%s^r^ to ^b^%s^r^%s", script, resolved.value, scopeInfo);
        String[] splitResolved = splitScript(resolved.value);
        for (String split : splitResolved) {
            int index = encountered.size() - 1;
            // TODO - how to propagate arguments? passed to each? passed to last?
            resolveExecutions(split, originalScript, scope, executions, encountered);
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
        execution = resolveExecutable(execution);
        execution = handleNonNativeExecutable(execution);
        String script = buildScriptName(execution.scriptArgs); // TODO - only if need be; augment Output to have way of computing values only if the log level is valid
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(execution.scriptArgs).redirectErrorStream(true).directory(projectRoot);
            Map<String, String> environment = processBuilder.environment();
            environment.putAll(Props.getPropsForEnv(execution.scope));
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
            Output.print("^error^ script ^green^%s^r^ failed [ result = %d ].", execution.originalScript, result);
        } catch (IOException ioe) {
            Output.print("^error^ executing script ^green^%s^r^", execution.originalScript);
            Output.print(ioe);
        } catch (InterruptedException ie) {
            Output.print(ie);
        }
        return false;
    }

    /**
     * Runs {@link org.moxie.ply.props.Props#filter(Prop)} on each value within {@code array}, the context will be scripts.
     * @param array to filter
     * @param scope of the {@code array} object's execution.
     */
    private static void filter(String[] array, String scope) {
        for (int i = 0; i < array.length; i++) {
            array[i] = Props.filterForPly(new Prop("aliases", "", "", array[i], true), scope);
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

    private static Execution resolveExecutable(Execution execution) {
        String passedInScript = execution.script;
        String script = execution.script;
        String localScriptsDir = Props.getValue("project", execution.scope, "scripts.dir");
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
     * @return the translated execution.
     */
    private static Execution handleNonNativeExecutable(Execution execution) {
        String script = execution.script;
        if (script.endsWith(".jar")) {
            return JarExec.createJarExecutable(execution);
        }
        return execution;
    }

}