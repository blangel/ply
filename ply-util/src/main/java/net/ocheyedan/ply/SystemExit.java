package net.ocheyedan.ply;

/**
 * User: blangel
 * Date: 1/16/12
 * Time: 9:56 AM
 *
 * Throw this exception to cause ply or the running script to exit.  Ply will catch this exception, run any shutdown
 * code and then call {@link System#exit(int)} with the provided {@link #exitCode}.
 */
@SuppressWarnings("serial")
public final class SystemExit extends RuntimeException {

    /**
     * Set internally by ply itself so that calls to {@link #exit(int)} throw an exception.
     */
    static volatile boolean ply = false;

    /**
     * If {@link #ply} then throw an {@link SystemExit} exception, otherwise, call {@link System#exit(int)}
     * with {@code exitCode}
     * @param exitCode to use when exiting.
     */
    public static void exit(int exitCode) {
        if (ply) {
            throw new SystemExit(exitCode);
        } else {
            System.exit(exitCode);
        }
    }

    public final int exitCode;

    public SystemExit(int exitCode) {
        this.exitCode = exitCode;
    }

    public SystemExit(int exitCode, Throwable cause) {
        super(cause);
        this.exitCode = exitCode;
    }

}
