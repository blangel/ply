package net.ocheyedan.ply.cmd.build;

import net.ocheyedan.ply.exec.Execution;

import java.util.ArrayList;
import java.util.List;

/**
 * User: blangel
 * Date: 1/15/12
 * Time: 10:50 AM
 *
 * Extends {@link Script} to allow for shell invocations.  Shell invocation within ply are denoted by surrounding
 * the script with '`' (tick marks).
 */
public final class ShellScript extends Script {

    ShellScript(Script script) {
        super(script.name, script.scope, script.arguments, script.unparsedName);
    }

    /**
     * Removes the tick marks from the script and creates an appropriate {@link Execution} object.
     * @param overriddenExecutionName to use in the converted {@link Execution} objects' {@link Execution#name} values.
     * @return an shell script execution
     */
    @Override protected List<Execution> convert(String overriddenExecutionName) {
        String[] executableArgs = new String[arguments.size() + 1];
        // remove the '`' leading and trailing tick marks
        String shellScript = name.substring(1, name.length() - 1);
        executableArgs[0] = shellScript;
        for (int i = 1; i < executableArgs.length; i++) {
            executableArgs[i] = arguments.get(i - 1);
        }
        List<Execution> executions = new ArrayList<Execution>(1);
        executions.add(new Execution(overriddenExecutionName, this, executableArgs));
        return executions;
    }
}
