package net.ocheyedan.ply;

import net.ocheyedan.ply.exec.Execution;
import net.ocheyedan.ply.props.Prop;
import net.ocheyedan.ply.props.Props;
import net.ocheyedan.ply.props.PropsExt;
import net.ocheyedan.ply.submodules.Submodule;
import net.ocheyedan.ply.submodules.Submodules;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * User: blangel
 * Date: 9/2/11
 * Time: 11:03 AM
 *
 * The main entry point.
 * Ply is intended to be invoked on a {@literal Unix} command line with the following options:
 * <pre>ply [--usage] <command></pre>
 * where {@literal --usage} prints the usage screen and command is either {@literal config} or {@literal init}
 *  or represents the actual chain of commands to invoke.
 * The options for {@literal config} are defined in {@link Config}.
 * The options for {@literal init} are defined in {@link Init}.
 */
public class Ply {

    public static void main(String args[]) {
        if ((args == null) || (args.length < 1)) {
            usage();
            System.exit(0);
        }
        AtomicReference<String[]> pureArgs = new AtomicReference<String[]>(args);
        AtomicReference<Map<String, Map<String, Prop>>> adHocProps = new AtomicReference<Map<String, Map<String, Prop>>>();
        PropsExt.splitArgs(pureArgs, adHocProps);
        args = pureArgs.get();
        if ("--usage".equals(args[0])) {
            PropsExt.updateAdHocProps(adHocProps.get());
            usage();
        } else if ("init".equals(args[0])) {
            PropsExt.updateAdHocProps(adHocProps.get());
            Init.invoke(args);
        } else if ("config".equals(args[0])) {
            checkAssumptions();
            PropsExt.updateAdHocProps(adHocProps.get());
            Config.invoke(args);
        } else {
            checkAssumptions();
            if (args.length > 0) {
                exec(args, adHocProps.get());
            } else {
                Output.print("^dbug^ Nothing to do, only -P arguments given.");
            }
        }
        System.exit(0);
    }

    /**
     * For {@code args} runs {@link Exec#resolve(java.io.File, String[], Map)} against the local project.  If
     * the local project has submodules then each submodule also has {@link Exec#resolve(java.io.File, String[], Map)}
     * invoked for {@code args}.  Then the resolved {@link Execution} objects are invoked via
     * {@link Exec#invoke(java.io.File, java.util.List)}
     * @param args are an array of unresolved aliases/scripts to be invoked for the project and all of its submodules
     * @param adHocProps the mapping of ad-hoc properties passed in by the invoker.
     */
    private static void exec(String[] args, Map<String, Map<String, Prop>> adHocProps) {
        // resolve before anything else in case '-Pply.decorated=false` has been set within an alias
        List<Execution> localExecutions = Exec.resolve(PlyUtil.LOCAL_PROJECT_DIR, args, adHocProps);

        long start = System.currentTimeMillis();
        String projectName = Props.getValue("project", "name");
        Output.printNoLine("^ply^ building ^b^%s^r^, %s", projectName, Props.getValue("project", "version"));

        String scope = Props.getValue("project", "submodules.scope");
        List<Submodule> orderedSubmodules = Submodules.getSubmodules(PlyUtil.LOCAL_CONFIG_DIR, scope);

        if ((orderedSubmodules != null) && !orderedSubmodules.isEmpty()) {
            Output.print(" and its submodules:");
            int maxSubmoduleName = projectName.length();
            float maxSubmoduleTime;
            for (Submodule submodule : orderedSubmodules) {
                Output.print("^ply^\t^b^%s^r^", submodule.name);
            }

            Map<String, Float> submodulesTimeMap = new LinkedHashMap<String, Float>(orderedSubmodules.size());

            // first run the args against the current project
            Output.print("^ply^");
            Output.print("^ply^ building ^b^%s^r^ itself before its submodules", projectName);
            long projectStart = System.currentTimeMillis();
            if (!Exec.invoke(PlyUtil.LOCAL_PROJECT_DIR, localExecutions)) {
                System.exit(1);
            }
            float seconds = printTime(projectStart, String.format("^b^%s^r^ ", projectName));
            maxSubmoduleTime = seconds;
            submodulesTimeMap.put(projectName, seconds);
            Output.print("^ply^");

            for (Submodule submodule : orderedSubmodules) {
                Output.print("^ply^ building ^b^%s^r^", submodule);
                if (submodule.name.length() > maxSubmoduleName) {
                    maxSubmoduleName = submodule.name.length();
                }
                long submoduleStart = System.currentTimeMillis();
                File submodulePlyDir = FileUtil.fromParts(PlyUtil.LOCAL_PROJECT_DIR.getPath(), "..", submodule.name, ".ply");
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
                List<Execution> submoduleExecutions = Exec.resolve(submodulePlyDir, args, adHocProps);
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

        } else {
            Output.print("");
            if (!Exec.invoke(PlyUtil.LOCAL_PROJECT_DIR, localExecutions)) {
                System.exit(1);
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
    private static float printTime(long start, String suppliment) {
        long end = System.currentTimeMillis();
        float seconds = ((end - start) / 1000.0f);
        long totalMem = Runtime.getRuntime().totalMemory() / 1024 / 1024;
        long freeMem = Runtime.getRuntime().freeMemory() / 1024 / 1024;
        Output.print("^ply^ Finished %sin ^b^%.3f seconds^r^ using ^b^%d/%d MB^r^.", suppliment, seconds, (totalMem - freeMem), totalMem);
        return seconds;
    }

    /**
     * Performs sanity checks on what ply assumes to exist.
     */
    private static void checkAssumptions() {
        if (!PlyUtil.SYSTEM_CONFIG_DIR.exists()) {
            Output.print("^error^ the ply install directory is corrupt, please re-install.");
            System.exit(1);
        }
        if (!PlyUtil.LOCAL_CONFIG_DIR.exists()) {
            Output.print("^warn^ not a ply project (or any of the parent directories), please initialize first: ^b^ply init^r^.");
            System.exit(1);
        }
    }

    private static void usage() {
        Output.print("ply [--usage] <^b^command^r^> [-PadHocProp...]");
        Output.print("  where ^b^command^r^ is either:");
        Output.print("    ^b^config^r^ <options>\t: see ^b^ply config --usage^r^");
        Output.print("    ^b^init^r^");
        Output.print("    <^b^build-scripts^r^>\t: a space delimited list of build scripts; i.e., ^b^ply clean \"myscript opt1\" compile test^r^");
        Output.print("  and ^b^-PadHocProp^r^ is zero to many ad-hoc properties prefixed with ^b^-P^r^ in the format ^b^context[#scope].propName=propValue^r^");
    }
}