package net.ocheyedan.ply.exec;

import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.SystemExit;
import net.ocheyedan.ply.cmd.build.ShellScript;
import net.ocheyedan.ply.props.Context;
import net.ocheyedan.ply.props.Props;

import java.io.File;

/**
 * User: blangel
 * Date: 1/15/12
 * Time: 10:39 AM
 */
public final class ShellExec {

    /**
     * Creates a shell execution based on {@code execution}.  The shell to use is defined by property {@literal scripts-sh.shell}
     * and is invoked with the {@literal scripts-sh.shell.args} options.
     * @param execution to invoke via the shell
     * @param configDirectory the configuration directory of this execution.
     * @return the shell execution
     */
    static Execution createShellExecutable(Execution execution, File configDirectory) {
        String executable = execution.executionArgs[0];
        String shell = Props.getValue(Context.named("scripts-sh"), "shell", configDirectory, execution.script.scope);
        String shellArgs = Props.getValue(Context.named("scripts-sh"), "shell.args", configDirectory, execution.script.scope);
        if (shell.isEmpty()) {
            Output.print("^error^ Cannot run '^b^%s^r^'. No ^b^shell^r^ property defined (^b^ply set shell=xxxx in scripts-sh^r^).",
                    executable);
            throw new SystemExit(1);
        }
        int supplimentalArgLength = 1 + (shellArgs.isEmpty() ? 0 : 1);
        String[] args = new String[supplimentalArgLength + execution.executionArgs.length];
        args[0] = shell;
        if (supplimentalArgLength > 1) {
            args[1] = shellArgs;
        }
        System.arraycopy(execution.executionArgs, 0, args, supplimentalArgLength, execution.executionArgs.length);
        if (execution.script instanceof ShellScript) {
            return execution.with(executable, args);
        } else {
            return execution.with(args);
        }
    }

    private ShellExec() { }

}
