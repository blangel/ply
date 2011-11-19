package net.ocheyedan.ply.script;

import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.PlyUtil;

import java.io.*;
import java.util.concurrent.locks.Lock;

/**
 * User: blangel
 * Date: 11/19/11
 * Time: 4:08 PM
 *
 * If the project hasn't already resolved dependencies locally and is not running with 'info' logging
 * it appears that ply has hung if downloading lots of dependencies.  This thread prints out a warning if not running
 * in 'info' logging and dependency resolution takes longer than 2 seconds.
 */
public final class SlowResolutionThread extends Thread {

    static final class Runner implements Runnable {

        final Lock lock;

        Runner(Lock lock) {
            this.lock = lock;
        }

        @Override public void run() {
            PrintStream old = null;
            try {
                Thread.sleep(2000);
                if (lock.tryLock()) {
                    try {
                        Output.print(
                                "^b^Yikes!^r^ Your project needs a lot of dependencies. Hang tight, ^b^ply^r^'s downloading them...");
                        if (PlyUtil.isHeadless()) {
                            Output.print("You can always run with ^b^-Pply.log.levels=info^r^ to see more log messages.");
                        } else {
                            // need to go directly to stdout to avoid Output parsing prior to Exec handling
                            System.out.println(String.format(
                                    "^no_line^You can always run with ^b^-Pply.log.levels=info^r^ to see more log messages. Enable now? [Y/n] "));
                            old = setupOutput(); // cursor's hanging on the last line of output, if more output comes need to prefix with newline
                        }
                    } finally {
                        lock.unlock();
                    }
                    // io-read doesn't interrupt, so need to release lock before attempting to read from
                    // stdin (fine anyway as we've already printed what needs to be printed, so being interrupted
                    // has no negative consequences [ as interruption means dependency-resolution has completed
                    // and so enabling 'info' log levels is spurious ]).
                    if (!PlyUtil.isHeadless()) {
                        try {
                            String answer = new BufferedReader(new InputStreamReader(System.in)).readLine();
                            if ((answer != null) && "y".equalsIgnoreCase(answer.trim())) {
                                Output.enableInfo();
                            }
                            revertOutput(old);
                        } catch (IOException ioe) {
                            throw new AssertionError(ioe);
                        }
                    }
                } else {
                    // dep-resolution has already completed, terminate
                }
            } catch (InterruptedException ie) {
                // abort
            }
        }

        /**
         * Causes first output to be printed on a newline .
         * @return the current {@link System#out} at time of this call.
         */
        private PrintStream setupOutput() {
            final PrintStream old = System.out;
            PrintStream tabbed = new PrintStream(new ByteArrayOutputStream() /* spurious as calls are delegated to 'old' */) {
                final Object[] nil = new Object[0];
                @Override public void print(String out) {
                    old.print(String.format("^no_prefix^%n%s", out));
                    System.setOut(old);
                }
                @Override public void println(String out) {
                    old.println(String.format("^no_prefix^%n%s", out));
                    System.setOut(old);
                }
            };
            System.setOut(tabbed);
            return old;
        }

        /**
         * Sets the {@link System#out} to {@code old}.
         * @param old the existing {@link PrintStream} before any call to {@link #setupOutput()}
         */
        private static void revertOutput(PrintStream old) {
            if (old != null) {
                System.setOut(old);
            }
        }
    }

    public SlowResolutionThread(Lock lock) {
        super(new Runner(lock));
        setDaemon(true);
    }
}
