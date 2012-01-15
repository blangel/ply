package net.ocheyedan.ply.ext.cmd.build;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.PlyUtil;
import net.ocheyedan.ply.ext.cmd.Args;
import net.ocheyedan.ply.ext.cmd.ReliantCommand;
import net.ocheyedan.ply.ext.exec.Exec;
import net.ocheyedan.ply.ext.exec.Execution;
import net.ocheyedan.ply.ext.props.Context;
import net.ocheyedan.ply.ext.props.Props;
import net.ocheyedan.ply.ext.submodules.Submodule;
import net.ocheyedan.ply.ext.submodules.Submodules;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

        List<Submodule> orderedSubmodules = Submodules.getSubmodules(PlyUtil.LOCAL_CONFIG_DIR);

        if ((orderedSubmodules == null) || orderedSubmodules.isEmpty()) {
            Output.print("");
            if (!Exec.invoke(PlyUtil.LOCAL_PROJECT_DIR, executions)) {
                System.exit(1);
            }
        } else {
            Output.print(" and its submodules:");
            for (Submodule submodule : orderedSubmodules) {
                Output.print("^ply^   ^b^%s^r^", submodule.name);
            }
            Map<String, Float> submodulesTimeMap = new LinkedHashMap<String, Float>(orderedSubmodules.size());

            // first run the args against the current project
            Output.print("^ply^");
            Output.print("^ply^ building ^b^%s^r^ itself before its submodules", projectName);
            long projectStart = System.currentTimeMillis();
            if (!Exec.invoke(PlyUtil.LOCAL_PROJECT_DIR, executions)) {
                System.exit(1);
            }
            int maxSubmoduleName = projectName.length();
            float seconds = printTime(projectStart, String.format("^b^%s^r^ ", projectName));
            float maxSubmoduleTime = seconds;
            submodulesTimeMap.put(projectName, seconds);
            Output.print("^ply^");

            for (Submodule submodule : orderedSubmodules) {
                Output.print("^ply^ building ^b^%s^r^", submodule);
                if (submodule.name.length() > maxSubmoduleName) {
                    maxSubmoduleName = submodule.name.length();
                }
                long submoduleStart = System.currentTimeMillis();
                File submodulePlyDir = FileUtil
                        .fromParts(PlyUtil.LOCAL_PROJECT_DIR.getPath(), "..", submodule.name, ".ply");
                if (!submodulePlyDir.exists()) {
                    File submoduleDir = FileUtil.fromParts(PlyUtil.LOCAL_PROJECT_DIR.getPath(), "..", submodule.name);
                    if (!submoduleDir.exists()) {
                        Output.print("^warn^ directory ^b^%s^r^ doesn't exist.", submodule.name);
                    } else {
                        Output.print("^warn^ submodule ^b^%s^r^ is not a ply project, skipping.", submodule.name);
                    }
                    Output.print("^ply^");
                    continue;
                }
                File submoduleConfigDir = FileUtil.fromParts(FileUtil.getCanonicalPath(submodulePlyDir), "config");
                List<Execution> submoduleExecutions = Module.resolve(args, submoduleConfigDir);
                if (!Exec.invoke(submodulePlyDir, submoduleExecutions)) {
                    System.exit(1);
                }
                seconds = printTime(submoduleStart, String.format("^b^%s^r^ ", submodule.name));
                if (seconds > maxSubmoduleTime) {
                    maxSubmoduleTime = seconds;
                }
                submodulesTimeMap.put(submodule.name, seconds);
                Output.print("^ply^");

            }

            Output.print("^ply^ Build Summary");
            maxSubmoduleName = Math.min(maxSubmoduleName, 80); // don't be ridiculous
            int maxTimeLength = String.valueOf(Float.valueOf(maxSubmoduleTime).intValue()).length();
            for (String module : submodulesTimeMap.keySet()) {
                int pad = Math.max(1, ((maxSubmoduleName + 1) - module.length()));
                float time = submodulesTimeMap.get(module);
                int timePad = Math.max(0, (maxTimeLength - String.valueOf(Float.valueOf(time).intValue()).length()));
                String timePadString = (timePad == 0 ? "" : String.valueOf(timePad));
                Output.print("^ply^ ^b^%s^r^%" + pad + "s%" + timePadString + "s^b^%.3f^r^ seconds", module, "", "", time);
            }

        }

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
