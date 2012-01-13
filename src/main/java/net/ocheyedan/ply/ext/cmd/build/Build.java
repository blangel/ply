package net.ocheyedan.ply.ext.cmd.build;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.PlyUtil;
import net.ocheyedan.ply.ext.cmd.Args;
import net.ocheyedan.ply.ext.cmd.Command;
import net.ocheyedan.ply.ext.cmd.ReliantCommand;
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
public final class Build extends ReliantCommand {

    public Build(Args args) {
        super(args);
    }

    @Override public void run() {
        super.run();
        List<Execution> executions = Module.resolve(args, PlyUtil.LOCAL_CONFIG_DIR);

        // print a statement quickly, alerting the user that the build has begun (need to do after local execution
        // resolution as that resolution may affect printing (i.e., ply.decorated=false from an alias definition).

        long start = System.currentTimeMillis();
        String projectName = Props.getLocalValue(Context.named("project"), "name");
        String projectVersion = Props.getLocalValue(Context.named("project"), "version");
        Output.printNoLine("^ply^ building ^b^%s^r^, %s", projectName, projectVersion);

        print(executions, "");

        printTime(start, "");
    }
    
    /**
     * Prints the amount of time used since {@code start} along with the memory usage.
     * @param start time of some task/execution/build
     * @param suppliment to indicate what has completed (should end with a blank space)
     * @return the amount of time in seconds since {@code start}
     */
    private float printTime(long start, String suppliment) {
        long end = System.currentTimeMillis();
        float seconds = ((end - start) / 1000.0f);
        long totalMem = Runtime.getRuntime().totalMemory() / 1024 / 1024;
        long freeMem = Runtime.getRuntime().freeMemory() / 1024 / 1024;
        Output.print("^ply^ Finished %sin ^b^%.3f seconds^r^ using ^b^%d/%d MB^r^.", suppliment, seconds, (totalMem - freeMem), totalMem);
        return seconds;
    }

    private void print(List<Execution> executions, String prefix) {
        for (Execution execution : executions) {
            Script script = execution.script;
            Output.print("%s^b^%s^r^ (scope = %s) : %s%s", prefix, script.name, script.scope.name, execution.executionArgs[0],
                    execution.executionArgs.length < 2 ? "" : String.format(" (args = %s)", script.arguments.toString()));
        }
    }

}
