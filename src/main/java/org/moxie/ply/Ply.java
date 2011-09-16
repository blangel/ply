package org.moxie.ply;

/**
 * User: blangel
 * Date: 9/2/11
 * Time: 11:03 AM
 *
 * The main entry point.
 * Ply is intended to be invoked on a {@literal Unix} command line with the following options:
 * <pre>ply [--usage] <command></pre>
 * where {@literal --usage} prints the usage screen and command is either {@literal config}
 *  or represents the actual chain of commands to invoke.
 * The options for {@literal config} are defined in {@link Config}.
 */
public class Ply {

    public static void main(String args[]) {
        if ((args == null) || (args.length < 1)) {
            usage();
            System.exit(0);
        }
        Output.initColor();
        if ("--usage".equals(args[0])) {
            usage();
        } else if ("config".equals(args[0])) {
            Config.invoke(args);
        } else {
            for (String script : args) {
                if (!Exec.invoke(script)) {
                    System.exit(1);
                }
            }
        }
        System.exit(0);
    }

    private static void usage() {
        Output.print("ply [--usage] <^b^command^r^>");
        Output.print("  where ^b^command^r^ is either:");
        Output.print("    ^b^config^r^ <options>\t: see ^b^ply config --usage^r^");
        Output.print("    <^b^build-scripts^r^>\t: a space delimited list of build scripts; i.e., ^b^ply clean \"myscript opt1\" compile test^r^");
    }

}
