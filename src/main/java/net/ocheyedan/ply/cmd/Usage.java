package net.ocheyedan.ply.cmd;

import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.OutputExt;

/**
 * User: blangel
 * Date: 12/30/11
 * Time: 2:49 PM
 *
 * A {@link Command} to print the usage information.
 */
public final class Usage extends Command {

    public Usage(Args args) {
        super(args);
    }

    @Override public void run() {
        OutputExt.init();
        Output.print("ply <^b^command^r^> [--usage|--help|--version] [-PadHocProp]");
        Output.print("  where ^b^command^r^ is either:");
        Output.print("    ^b^init^r^");
        Output.print("        initializes the current directory as a ply project");
        Output.print("    ^b^update^r^");
        Output.print("        checks for updates to the ply system itself");
        Output.print("    ^b^get^r^ [propName] [from ^b^context^r^] [--unfiltered]");
        Output.print("        lists all project properties or, if specified, those like '^b^propName^r^' within '^b^context^r^'");
        Output.print("    ^b^get-all^r^ [propName] [from ^b^context^r^] [--unfiltered]");
        Output.print("        same as the '^b^get^r^' command but also lists non-project properties (i.e., system defaults).");
        Output.print("    ^b^set^r^ propName=propValue in ^b^context^r^");
        Output.print("        sets '^b^propName^r^' equal to '^b^propValue^r^' within '^b^context^r^' for the project");
        Output.print("    ^b^append^r^ propValue to propName in ^b^context^r^");
        Output.print("        appends '^b^propValue^r^' to the value of '^b^propName^r^' within '^b^context^r^' for the project");
        Output.print("    ^b^prepend^r^ propValue to propName in ^b^context^r^");
        Output.print("        prepends '^b^propValue^r^' to the value of '^b^propName^r^' within '^b^context^r^' for the project");
        Output.print("    ^b^rm^r^ propName from ^b^context^r^");
        Output.print("        removes '^b^propName^b^' from '^b^context^r^' for the project");
        Output.print("    <^b^build-scripts^r^>");
        Output.print("        a space delimited list of build scripts; i.e., ^b^ply clean \"myscript opt1\" compile test^r^");
        Output.print("  and ^b^-PadHocProp^r^ is zero to many ad-hoc properties prefixed with ^b^-P^r^ in the format ^b^context[#scope].propName=propValue^r^");
    }

}
