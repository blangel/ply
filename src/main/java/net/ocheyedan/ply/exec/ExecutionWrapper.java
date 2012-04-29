package net.ocheyedan.ply.exec;

import net.ocheyedan.ply.Output;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * User: blangel
 * Date: 4/29/12
 * Time: 12:30 PM
 *
 * Wraps {@link Execution} objects for use by {@link Exec}.  This encapsulates things like printing debug output, exception
 * handling, etc.
 */
final class ExecutionWrapper {

    /**
     * The epoch-time at which the {@link Process} was started.
     */
    private final long start;

    /**
     * The long-form script name (i.e., might include the full {@literal JVM} command-line execution).
     */
    private final String scriptName;

    /**
     * The short-form script name (e.g., clean).
     */
    private final String outputScriptName;

    /**
     * The wrapped execution object.
     */
    final Execution execution;

    /**
     * Whether invocation produced an error.
     */
    private final AtomicBoolean errorOnInvoke;

    ExecutionWrapper(Execution execution, long start) {
        this.execution = execution;
        this.start = start;
        this.scriptName = Output.isDebug() ? buildScriptName(execution.executionArgs) : "";
        this.outputScriptName = buildExecutionName(execution);
        this.errorOnInvoke = new AtomicBoolean(false);
    }

    /**
     * Wraps error handling around calls to {@link Execution#invoke(String)}
     */
    void invoke() {
        try {
            execution.invoke(scriptName);
        } catch (IOException ioe) {
            errorOnInvoke.set(true);
            Output.print("^error^ executing script ^green^%s^r^", execution.script.unparsedName);
            Output.print(ioe);
        }
    }

    /**
     * Wraps error handling around calls to {@link Execution#waitFor(String)} as well as prints inforation about failure,
     * and success running time.
     * @return true if the execution completed successfully, false otherwise.
     */
    boolean waitFor() {
        if (errorOnInvoke.get()) {
            return false;
        }
        try {
            int result = execution.waitFor(outputScriptName);
            printTime(start, outputScriptName);
            if (result == 0) {
                return true;
            }
            Output.print("^error^ script ^green^%s^r^ failed [ exit code = %d ].", execution.script.unparsedName, result);
        } catch (IOException ioe) {
            Output.print("^error^ executing script ^green^%s^r^", execution.script.unparsedName);
            Output.print(ioe);
        } catch (InterruptedException ie) {
            Output.print(ie);
        }
        return false;
    }

    private static float printTime(long start, String script) {
        long end = System.currentTimeMillis();
        float seconds = ((end - start) / 1000.0f);
        Output.print("^dbug^ executed ^b^%s^r^ in ^b^%.3f seconds^r^.", script, seconds);
        return seconds;
    }

    private static String buildExecutionName(Execution execution) {
        String name = execution.name;
        String scope = execution.script.scope.getScriptPrefix();
        // only prefix with scope if the execution name isn't the same as the scope
        if (!name.equals(execution.script.scope.name)) {
            return scope + name;
        } else {
            return name;
        }
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
