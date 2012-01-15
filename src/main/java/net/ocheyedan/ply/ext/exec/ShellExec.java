package net.ocheyedan.ply.ext.exec;

import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.ext.cmd.build.Script;
import net.ocheyedan.ply.ext.props.Context;
import net.ocheyedan.ply.ext.props.Props;

import java.io.File;
import java.util.List;

/**
 * User: blangel
 * Date: 1/15/12
 * Time: 10:39 AM
 */
public final class ShellExec {

    /**
     * Creates a shell execution based on {@code execution}.  The shell to use is defined by property {@literal ply.shell}
     * and is invoked with the {@literal -c} option.
     * @param execution to invoke via the shell
     * @param configDirectory the configuration directory of this execution.
     * @return the shell execution
     */
    static Execution createShellExecutable(Execution execution, File configDirectory) {
        String executable = execution.executionArgs[0];
        String shell = Props.getValue(Context.named("ply"), "shell", configDirectory, execution.script.scope);
        if (shell.isEmpty()) {
            Output.print("^error^ No ^b^shell^r^ property defined in context ^b^ply^r^. Define to run ^b^%s^r^",
                    executable);
            System.exit(1);
        }
        String[] args = new String[2 + execution.executionArgs.length];
        args[0] = shell;
        args[1] = "-c";
        System.arraycopy(execution.executionArgs, 0, args, 2, execution.executionArgs.length);
        return execution.with(executable, args);
    }

    private ShellExec() { }

}
