package net.ocheyedan.ply.exec;

import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * User: blangel
 * Date: 11/19/11
 * Time: 4:23 PM
 *
 * Pipes {@link System#in} to the {@link Runner#processStdin}.
 *
 * This class can be instantiated once for all executions.  Multiple calls to {@link #start()} have no negative
 * effect; the first call actually starts and all subsequent calls are no-ops.
 */
final class StdinProcessPipe extends Thread {

    static final class Runner implements Runnable {

        final AtomicReference<OutputStream> processStdin;

        Runner(AtomicReference<OutputStream> processStdin) {
            this.processStdin = processStdin;
        }

        // TODO - should only slurp input if requested by our child process
        // TODO - otherwise, if ply is being invoked from within the context of a script (loop) reading input
        // TODO - ply will consume the input from the parent process
        @Override public void run() {
            byte[] buffer = new byte[4096];
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    int avail = System.in.available();
                    if (avail > 0) {
                        int read = System.in.read(buffer, 0, Math.min(avail, buffer.length));
                        OutputStream stdin = processStdin.get();
                        if (stdin != null) {
                            stdin.write(buffer, 0, read);
                            stdin.flush();
                        }
                    } else {
                        Thread.sleep(100);
                    }
                } catch (IOException ioe) {
                    if (ioe.getMessage().contains("Broken pipe")) { // old process died
                        processStdin.set(null);
                    } else {
                        throw new AssertionError(ioe);
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    final AtomicBoolean started;

    final AtomicReference<OutputStream> processStdin;

    public StdinProcessPipe() {
        this(new AtomicReference<OutputStream>(null));
    }

    private StdinProcessPipe(AtomicReference<OutputStream> processStdin) {
        super(new Runner(processStdin));
        setDaemon(true);
        started = new AtomicBoolean(false);
        this.processStdin = processStdin;
    }

    void write(char[] buffer) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(this.processStdin.get()));
        writer.write(buffer, 0, buffer.length);
        writer.write('\n');
        writer.flush();
    }

    public void pausePipe() {
        this.processStdin.set(null);
    }

    public void startPipe(OutputStream processStdin) {
        this.processStdin.set(processStdin);
        start();
    }

    @Override public void start() {
        if (!started.getAndSet(true)) {
            super.start();
        }
    }
}
