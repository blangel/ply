package org.moxie.ply;

import org.moxie.ply.props.Props;

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
        // init the output information
        String colorProp = Props.getValue("color");
        boolean color = (colorProp == null || !"false".equals(colorProp));
        String logLevelsProp = Props.getValue("log.levels");
        Output.init(color, logLevelsProp);
        if ("--usage".equals(args[0])) {
            usage();
        } else if ("config".equals(args[0])) {
            Config.invoke(args);
        } else if ("init".equals(args[0])) {
            Init.invoke(args);
        } else {
            long start = System.currentTimeMillis();
            Output.print("^ply^ building ^b^" + Props.getValue("project", "name") + "^r^, " +
                    Props.getValue("project", "version"));
            for (String script : args) {
                if (!Exec.invoke(script)) {
                    System.exit(1);
                }
            }
            long end = System.currentTimeMillis();
            float seconds = ((end - start) / 1000.0f);
            long totalMem = Runtime.getRuntime().totalMemory() / 1024 / 1024;
            long freeMem = Runtime.getRuntime().freeMemory() / 1024 / 1024;
            Output.print("^ply^ Finished in ^b^%.3f seconds^r^ using ^b^%d/%d MB^r^.", seconds, (totalMem - freeMem), totalMem);
        }
        System.exit(0);
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