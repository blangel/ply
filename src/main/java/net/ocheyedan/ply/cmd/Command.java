package net.ocheyedan.ply.cmd;

import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.PlyUtil;
import net.ocheyedan.ply.SystemExit;

/**
 * User: blangel
 * Date: 12/22/11
 * Time: 11:03 AM
 *
 * Represents a command given to Ply from the user via the command line.
 */
public abstract class Command implements Runnable {

    /**
     * Represents {@link Command} implementations which have assumptions which need to be met to be executed.  This class
     * ensures that the {@link PlyUtil#SYSTEM_CONFIG_DIR} directory exists.
     */
    public abstract static class SystemReliant extends Command {

        protected SystemReliant(Args args) {
            super(args);
        }

        @Override public final void run() {
            runBeforeAssumptionsCheck();
            checkAssumptions();
            runAfterAssumptionsCheck();
        }

        /**
         * The implementations' {@link #run()} method to implement.  This will ensure implementations don't forget
         * to call {@link super#run()} which will likely circumvent the {@link #checkAssumptions()} call.
         */
        protected abstract void runAfterAssumptionsCheck();

        /**
         * The implementations' hook into doing something before the {@link #checkAssumptions()} is done.
         */
        protected abstract void runBeforeAssumptionsCheck();

        /**
         * Performs sanity checks that the ply system configuration directory exists.
         */
        protected void checkAssumptions() {
            if (!PlyUtil.SYSTEM_CONFIG_DIR.exists()) {
                Output.print("^error^ the ply install directory is corrupt, please re-install.");
                throw new SystemExit(1);
            }
        }
    }

    /**
     * Represents {@link Command.SystemReliant} implementations which has an additional assumption.  This class
     * ensures that the {@link net.ocheyedan.ply.PlyUtil#SYSTEM_CONFIG_DIR} and
     * {@link net.ocheyedan.ply.PlyUtil#LOCAL_CONFIG_DIR} directories exist.
     */
    public abstract static class ProjectReliant extends SystemReliant {
        
        protected ProjectReliant(Args args) {
            super(args);
        }

        /**
         * Performs sanity checks on what ply assumes to exist.
         */
        @Override
        protected void checkAssumptions() {
            super.checkAssumptions();
            if (!PlyUtil.LOCAL_CONFIG_DIR.exists()) {
                Output.print(
                        "^warn^ not a ply project (or any of the parent directories), please initialize first: ^b^ply init^r^.");
                throw new SystemExit(1);
            }
        }
    }

    public final Args args;

    protected Command(Args args) {
        this.args = args;
    }

}