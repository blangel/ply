package net.ocheyedan.ply.ext.cmd;

import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.PlyUtil;

/**
 * User: blangel
 * Date: 1/13/12
 * Time: 10:10 AM
 *
 * Represents {@link Command} implementations which have assumptions which need to be met to be executed.  This class
 * ensures that the {@link PlyUtil#SYSTEM_CONFIG_DIR} and {@link PlyUtil#LOCAL_CONFIG_DIR} directories exist.
 */
public abstract class ReliantCommand extends Command {

    protected ReliantCommand(Args args) {
        super(args);
    }

    @Override public void run() {
        checkAssumptions();
    }

    /**
     * Performs sanity checks on what ply assumes to exist.
     */
    private void checkAssumptions() {
        if (!PlyUtil.SYSTEM_CONFIG_DIR.exists()) {
            Output.print("^error^ the ply install directory is corrupt, please re-install.");
            System.exit(1);
        }
        if (!PlyUtil.LOCAL_CONFIG_DIR.exists()) {
            Output.print("^warn^ not a ply project (or any of the parent directories), please initialize first: ^b^ply init^r^.");
            System.exit(1);
        }
    }

}
