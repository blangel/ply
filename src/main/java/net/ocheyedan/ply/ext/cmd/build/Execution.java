package net.ocheyedan.ply.ext.cmd.build;

import java.util.Arrays;

/**
 * User: blangel
 * Date: 1/12/12
 * Time: 9:42 AM
 *
 * Represents a concrete execution.  Unlike, {@link Script}, this class represents something which can be executed
 * via a process.  To make a {@link Script} executable it is necessary to resolve its invoking mechanism; say direct,
 * via a JVM, etc.
 */
final class Execution {

    /**
     * The underlying script for this execution.
     */
    final Script script;

    /**
     * The arguments to the execution for the script.  By convention of {@link Process} the
     * first item is the execution itself (i.e., 'java').  So for an execution without args this
     * array has one value, the execution itself.
     */
    final String[] executionArgs;

    Execution(Script script, String[] executionArgs) {
        this.script = script;
        this.executionArgs = executionArgs;
    }

    Execution augment(String[] with) {
        String[] args = new String[this.executionArgs.length + with.length];
        System.arraycopy(this.executionArgs, 0, args, 0, this.executionArgs.length);
        System.arraycopy(with, 0, args, this.executionArgs.length, with.length);
        return new Execution(script, args);
    }

    @Override public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Execution execution = (Execution) o;

        if (script != null ? !script.equals(execution.script) : execution.script != null) {
            return false;
        }
        return Arrays.equals(executionArgs, execution.executionArgs);
    }

    @Override public int hashCode() {
        int result = script != null ? script.hashCode() : 0;
        result = 31 * result + Arrays.hashCode(executionArgs);
        return result;
    }
}
