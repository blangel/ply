package net.ocheyedan.ply.cmd;

import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.OutputExt;
import net.ocheyedan.ply.PlyUtil;

/**
 * User: blangel
 * Date: 1/19/12
 * Time: 6:05 PM
 *
 * Prints version information.
 */
public class Version extends Command {

    public Version(Args args) {
        super(args);
    }

    @Override public void run() {
        OutputExt.init(); // dis-regard ad-hoc props and defined properties, simply init
        Output.print("^ply^ ^b^ply^r^ version ^b^%s^r^", PlyUtil.PLY_VERSION);
        Output.print("^ply^   installed at ^b^%s^r^", PlyUtil.INSTALL_DIRECTORY);
        Output.print("^ply^  with java version ^b^%s^r^ from vendor ^b^%s^r^", System.getProperty("java.version"),
                                                              System.getProperty("java.vendor"));
        Output.print("^ply^   installed at ^b^%s^r^", System.getProperty("java.home"));
        Output.print("^ply^  and operating-system ^b^%s^r^ version ^b^%s^r^ arch ^b^%s^r^", System.getProperty("os.name"),
                                                                     System.getProperty("os.version"),
                                                                     System.getProperty("os.arch"));

    }
}
