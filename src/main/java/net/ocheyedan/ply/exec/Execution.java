package net.ocheyedan.ply.exec;

/**
 * User: blangel
 * Date: 11/13/11
 * Time: 2:02 PM
 *
 * A struct of execution information.
 */
public class Execution {

    /**
     * The original, unresolved, script/alias.
     */
    public final String originalScript;

    /**
     * The scope of this execution.
     */
    public final String scope;

    /**
     * The resolved script.
     */
    public final String script;

    /**
     * The arguments to the execution for the script.  By convention of {@link Process} the
     * first item is the {@link #script} itself.  So for an execution without args this
     * array has one value, the script to execute.
     */
    public final String[] scriptArgs;

    public Execution(String originalScript, String scope, String script, String[] scriptArgs) {
        this.originalScript = originalScript;
        this.scope = scope;
        this.script = script;
        this.scriptArgs = scriptArgs;
    }

    public Execution with(String script) {
        String[] args = new String[this.scriptArgs.length];
        System.arraycopy(this.scriptArgs, 1, args, 1, this.scriptArgs.length - 1);
        args[0] = script;
        return new Execution(this.originalScript, this.scope, script, args);
    }

    public Execution with(String[] args) {
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
