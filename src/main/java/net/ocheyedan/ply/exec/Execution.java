package net.ocheyedan.ply.exec;

import net.ocheyedan.ply.cmd.build.Script;

import java.util.Arrays;

/**
 * User: blangel
 * Date: 11/13/11
 * Time: 2:02 PM
 *
 * Represents a concrete execution.  Unlike, {@link net.ocheyedan.ply.cmd.build.Script}, this class represents something which can be executed
 * via a {@link Process}.  To make a {@link net.ocheyedan.ply.cmd.build.Script} executable it is necessary to resolve its invoking mechanism;
 * say direct, via a JVM, etc.
 */
public final class Execution {

    /**
     * A name to use when identifying this exeuction.
     */
    public final String name;

    /**
     * The underlying script for this execution.
     */
    public final Script script;

    /**
     * The arguments to the execution for the script.  By convention of {@link Process} the
     * first item is the execution itself (i.e., 'java').  So for an execution without args this
     * array has one value, the execution itself.
     */
    public final String[] executionArgs;

    public Execution(String name, Script script, String[] executionArgs) {
        this.name = name;
        this.script = script;
        this.executionArgs = executionArgs;
    }

    public Execution augment(String[] with) {
        String[] args = new String[this.executionArgs.length + with.length];
        System.arraycopy(this.executionArgs, 0, args, 0, this.executionArgs.length);
        System.arraycopy(with, 0, args, this.executionArgs.length, with.length);
        return new Execution(name, script, args);
    }

    public Execution with(String executable) {
        String[] args = new String[this.executionArgs.length];
        System.arraycopy(this.executionArgs, 1, args, 1, this.executionArgs.length - 1);
        args[0] = executable;
        return new Execution(name, this.script, args);
    }

    public Execution with(String[] args) {
        return new Execution(name, this.script, args);
    }

    public Execution with(String executionName, String[] args) {
        return new Execution(executionName, this.script, args);
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
