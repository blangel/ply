package org.moxie.ply;

import org.moxie.ply.props.Prop;
import org.moxie.ply.props.Props;
import org.moxie.ply.submodules.Submodule;
import org.moxie.ply.submodules.Submodules;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        checkAssumptions("init".equals(args[0]));
        if ("--usage".equals(args[0])) {
            usage();
        } else if ("config".equals(args[0])) {
            Config.invoke(args);
        } else if ("init".equals(args[0])) {
            Init.invoke(args);
        } else {
            exec(args);
        }
        System.exit(0);
    }

    private static void exec(String[] args) {
        long start = System.currentTimeMillis();
        String projectName = Props.getValue("project", "name");
        Output.printNoLine("^ply^ building ^b^%s^r^, %s", projectName, Props.getValue("project", "version"));

        String scope = Props.getValue("project", "submodules.scope");
        Map<String, Prop> submodules = Submodules.getSubmodules(PlyUtil.LOCAL_CONFIG_DIR, scope);

        if ((submodules != null) && !submodules.isEmpty()) {
            Output.print(" and its submodules:");
            List<Submodule> orderedSubmodules = Submodules.sortByDependencies(submodules.values(), scope);
            int maxSubmoduleName = projectName.length();
            float maxSubmoduleTime = 0.0f;
            for (Submodule submodule : orderedSubmodules) {
                Output.print("^ply^\t^b^%s^r^", submodule.name);
            }

            Map<String, Float> submodulesTimeMap = new LinkedHashMap<String, Float>();

            // first run the args against the current project
            Output.print("^ply^");
            Output.print("^ply^ building ^b^%s^r^ itself before its submodules", projectName);
            long projectStart = System.currentTimeMillis();
            for (String script : args) {
                if (!Exec.invoke(PlyUtil.LOCAL_PROJECT_DIR, script)) {
                    System.exit(1);
                }
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
                for (String script : args) {
                    if (!Exec.invoke(submodulePlyDir, script)) {
                        System.exit(1);
                    }
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
            for (String script : args) {
                if (!Exec.invoke(PlyUtil.LOCAL_PROJECT_DIR, script)) {
                    System.exit(1);
                }
            }
        }

        printTime(start, "");
    }

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
     * @param init true if current invocation is running 'init' (meaning don't fail because the project hasn't been init-ed).
     */
    private static void checkAssumptions(boolean init) {
        if (!PlyUtil.SYSTEM_CONFIG_DIR.exists()) {
            Output.print("^error^ the ply install directory is corrupt, please re-install.");
            System.exit(1);
        }
        if (!init && !PlyUtil.LOCAL_CONFIG_DIR.exists()) {
            Output.print("^warn^ not a ply project (or any of the parent directories), please initialize first: ^b^ply init^r^.");
            System.exit(1);
        }
    }

    private static void usage() {
        Output.print("ply [--usage] <^b^command^r^>");
        Output.print("  where ^b^command^r^ is either:");
        Output.print("    ^b^config^r^ <options>\t: see ^b^ply config --usage^r^");
        Output.print("    ^b^init^r^");
        Output.print("    <^b^build-scripts^r^>\t: a space delimited list of build scripts; i.e., ^b^ply clean \"myscript opt1\" compile test^r^");
    }
}