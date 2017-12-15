package net.ocheyedan.ply;

/**
 * User: blangel
 * Date: 3/20/15
 * Time: 12:55 PM
 */
public class ShutdownHandler extends Thread {

    public ShutdownHandler() {
        super(new Runnable() {
            @Override public void run() {
                // ensure project.ply.invocation.dir is removed after invocation
                try {
                    PlyUtil.cleanupInvocationProperties();
                } catch (SystemExit se) {
                    // ignore; shutting down anyway
                }
            }
        });
    }
}
