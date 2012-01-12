package net.ocheyedan.ply.ext.cmd.build;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.PlyUtil;
import net.ocheyedan.ply.ext.cmd.Args;
import net.ocheyedan.ply.ext.cmd.Command;
import net.ocheyedan.ply.ext.props.Context;
import net.ocheyedan.ply.ext.props.Prop;
import net.ocheyedan.ply.ext.props.Props;
import net.ocheyedan.ply.ext.props.Scope;

import java.io.File;
import java.util.*;

/**
 * User: blangel
 * Date: 12/30/11
 * Time: 2:50 PM
 *
 * A {@link net.ocheyedan.ply.ext.cmd.Command} to handle running build scripts.
 * This is where arguments are translated to scripts and resolved against aliases.  After this translation,
 * the scripts are turned into executions and invoked.
 */
public final class Build extends Command {

    public Build(Args args) {
        super(args);
    }

    public void run() {
        List<Execution> executions = Module.resolve(args, PlyUtil.LOCAL_CONFIG_DIR);
        print(executions, "");
    }

    private void print(List<Execution> executions, String prefix) {
        for (Execution execution : executions) {
            Script script = execution.script;
            Output.print("%s^b^%s^r^ (scope = %s) : %s%s", prefix, script.name, script.scope.name, execution.executionArgs[0],
                    execution.executionArgs.length < 2 ? "" : String.format(" (args = %s)", script.arguments.toString()));
        }
    }

}
