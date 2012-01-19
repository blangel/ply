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
     * Note, any {@link #arguments} are concatenated (space delimited) into a string and appended to the end of the
     * {@link #name} as the shell invocation's arguments are the argument to the resolved shell (i.e., bash) and
     * not considered part of the actual scripts arguments (i.e., ls).
     * @param overriddenExecutionName to use in the converted {@link Execution} objects' {@link Execution#name} values.
     * @return an shell script execution
     */
    @Override protected List<Execution> convert(String overriddenExecutionName) {
        String[] executableArgs = new String[1];
        // remove the '`' leading and trailing tick marks
        StringBuilder shellScript = new StringBuilder(name.substring(1, name.length() - 1));
        for (String arg : arguments) {
            shellScript.append(" ");
            shellScript.append(arg);
        }
        executableArgs[0] = shellScript.toString();
        List<Execution> executions = new ArrayList<Execution>(1);
        executions.add(new Execution(overriddenExecutionName, this, executableArgs));
        return executions;
    }
}
