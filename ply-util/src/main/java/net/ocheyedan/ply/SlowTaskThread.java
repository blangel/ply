package net.ocheyedan.ply;

import net.ocheyedan.ply.input.InterruptibleInputReader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * User: blangel
 * Date: 2/10/12
 * Time: 1:45 PM
 * 
 * Some tasks can be slow (i.e., downloading a large dependency graph).  These tasks can make it seem that ply is
 * hung when in fact it is working, its just that no log messages are being printed.  For such tasks, this
 * thread can be invoked where a warning appears to the user after a set number of seconds to alert the user of
 * what is happening.
 */
public final class SlowTaskThread {
    
    /**
     * Builder pattern to configure a {@link SlowTaskThread}. 
     * @param <T> type of the return result of the {@link Callable}
     */
    public static final class BuilderStart<T> {
        
        private final long ms;
        
        private BuilderStart(long ms) {
            this.ms = ms;
        }
        
        public BuilderOngoing<T> warn(String warning) {
            return new BuilderOngoing<T>(this, warning);
        }
        
    }

    /**
     * Provides optional configuration options to the user.
     * @param <T> type of the return result of the {@link Callable}
     */
    public static final class BuilderOngoing<T> {
        
        private static enum Logging {
            Warn("You can always run with ^b^-Pply.log.levels=warn^r^ to see more log messages."), 
            Info("You can always run with ^b^-Pply.log.levels=info^r^ to see more log messages."), 
            Debug("You can always run with ^b^-Pply.log.levels=debug^r^ to see more log messages.");
            
            private final String message;
            
            private Logging(String message) {
                this.message = message;
            }
            
            private boolean isLoggingEnabled() {
                switch (this) {
                    case Warn:
                        return Output.isWarn();
                    case Info:
                        return Output.isInfo();
                    case Debug:
                        return Output.isDebug();
                    default:
                        throw new AssertionError(String.format("Unknown logging type %s", this.name()));
                }
            }

            private void enableLogging() {
                switch (this) {
                    case Warn:
                        Output.enableWarn(); break;
                    case Info:
                        Output.enableInfo(); break;
                    case Debug:
                        Output.enableDebug(); break;
                    default:
                        throw new AssertionError(String.format("Unknown logging type %s", this.name()));
                }
            }
        }
        
        private final BuilderStart<T> builder;
        
        private final String warning;
        
        private final AtomicReference<Logging> logging;

        private final AtomicBoolean ignoreIfHeadless;

        private BuilderOngoing(BuilderStart<T> builder, String warning) {
            this.builder = builder;
            this.warning = warning;
            this.logging = new AtomicReference<Logging>(Logging.Warn);
            this.ignoreIfHeadless = new AtomicBoolean(true);
        }

        /**
         * Will only invoke the {@link SlowTaskThread} if {@link Output#isInfo()} returns false,
         * The default is to only invoke if {@link Output#isWarn()} returns false.
         * @return this for method chaining
         */
        public BuilderOngoing<T> onlyIfNotLoggingInfo() {
            this.logging.set(Logging.Info);
            return this;
        }

        /**
         * Will only invoke the {@link SlowTaskThread} if {@link Output#isDebug()} returns false.
         * The default is to only invoke if {@link Output#isWarn()} returns false.
         * @return this for method chaining
         */
        public BuilderOngoing<T> onlyIfNotLoggingDebug() {
            this.logging.set(Logging.Debug);
            return this;
        }

        /**
         * Will cause the {@link SlowTaskThread} to print the {@link BuilderOngoing#warning} even if 
         * {@link PlyUtil#isHeadless()} is false (the default is the opposite).
         * @return this for method chaining.
         */
        public BuilderOngoing<T> evenIfHeadless() {
            this.ignoreIfHeadless.set(false);
            return this;
        }

        /**
         * @param task is the {@link Callable} to actually execute with a {@link SlowTaskThread}
         * @return the final builder which can be used to actually execute the {@code task}
         */
        public BuilderEnd<T> whenDoing(Callable<T> task) {
            return new BuilderEnd<T>(this, task);
        }

    }

    /**
     * The final builder which will allow the user to actually execute the task.
     * @param <T> type of the return result of the {@link Callable}
     */
    public static final class BuilderEnd<T> {
        
        private final BuilderOngoing<T> builder;

        private final Callable<T> task;

        private BuilderEnd(BuilderOngoing<T> builder, Callable<T> task) {
            this.builder = builder;
            this.task = task;
        }

        /**
         * Actually executes the task {@link Callable#call()} and returns the result.
         * Note, the {@link BuilderOngoing#warning} will be printed after {@link net.ocheyedan.ply.SlowTaskThread.BuilderStart#ms} only if
         * the task, {@link BuilderEnd#task}, has not already completed.
         * @return the result of executing the task.
         */
        public T start() {
            Thread slowTaskThread = null;
            if (!builder.logging.get().isLoggingEnabled()
                    // if not ignoring headless (so both headless and not headless are valid) or if not headless
                    && (!builder.ignoreIfHeadless.get() || !PlyUtil.isHeadless())) {
                slowTaskThread = new SlowTaskThreadImpl(builder);
                slowTaskThread.start();
            }
            T result = null;
            try {
                result = task.call();
            } catch (Exception e) {
                Output.print(e);
            } finally {
                if (slowTaskThread != null) {
                    slowTaskThread.interrupt();
                }
            }
            return result;
        }
    }

    /**
     * The actual {@link Runnable} implementation for {@link SlowTaskThreadImpl}
     */
    private static final class Runner implements Runnable {

        /**
         * Indicates whether the user has responded to the log-prompt (if !PlyUtil.isHeadless())
         */
        private final AtomicBoolean responded;

        private final String message;

        private final long wait;
        
        private final String logMessage;

        private final BuilderOngoing.Logging logging;

        Runner(BuilderOngoing<?> builder) {
            this.responded = new AtomicBoolean(true);
            this.message = builder.warning;
            this.wait = builder.builder.ms;
            this.logMessage = builder.logging.get().message;
            this.logging = builder.logging.get();
        }

        @Override public void run() {
            try {
                Thread.sleep(wait);
                if (!Thread.currentThread().isInterrupted()) {
                    Output.print(message);
                    // shouldn't be run if headless; but just in case.
                    if (PlyUtil.isHeadless()) {
                        Output.print(logMessage);
                    } else {
                        responded.set(false);
                        // need to go directly to stdout to avoid Output parsing prior to Exec handling
                        System.out.println(String.format("^no_line^%s Enable now? [Y/n] ", logMessage));
                        setupOutput(); // cursor's hanging on the last line of output, if more output comes need to prefix with newline
                    }
                    // io-read doesn't interrupt, so need to continuously poll availability of bytes on the stdin
                    // socket via InterruptibleInputReader.
                    if (!PlyUtil.isHeadless()) {
                        InterruptibleInputReader reader = new InterruptibleInputReader(System.in);
                        try {
                            String answer = reader.readLine();
                            responded.set(true);
                            if ((answer != null) && "y".equalsIgnoreCase(answer.trim())) {
                                logging.enableLogging();
                            }
                        } catch (IOException ioe) {
                            throw new AssertionError(ioe);
                        }
                    }
                } else {
                    // task has already completed, terminate
                }
            } catch (InterruptedException ie) {
                // abort (cancellation is set by thread)
            } catch (InterruptibleInputReader.InterruptedRuntimeException ire) {
                // abort (cancellation is set by thrower)
            }
        }

        /**
         * Causes first output to be printed on a newline .
         * @return the current {@link System#out} at time of this call.
         */
        private PrintStream setupOutput() {
            final PrintStream old = System.out;
            final AtomicBoolean revertedOut = new AtomicBoolean(false);
            PrintStream tabbed = new PrintStream(new ByteArrayOutputStream() /* spurious as calls are delegated to 'old' */) {
                final Object[] nil = new Object[0];
                @Override public void print(String out) {
                    if (revertedOut.getAndSet(true)) { // only print newline once
                        old.print(out);
                        return;
                    }
                    if (responded.get()) {
                        old.print(out);
                    } else {
                        old.print(String.format("^no_prefix^%n%s", out));
                    }
                    System.setOut(old);
                }
                @Override public void println(String out) {
                    if (revertedOut.getAndSet(true)) { // only print newline once
                        old.println(out);
                        return;
                    }
                    if (responded.get()) {
                        old.println(out);
                    } else {
                        old.println(String.format("^no_prefix^%n%s", out));
                    }
                    System.setOut(old);
                }
            };
            System.setOut(tabbed);
            return old;
        }

        /**
         * Ensures a new line has been printed.  There might not be a new line if 
         * {@link #responded} is false as this implies the user hasn't responded to the question asked by ply
         * about enabling more log levels.
         */
        private void ensureNewLine() {
            if (!responded.get()) {
                // user hasn't responded, need to jump lines for all future output
                // note, at this point {@link System#out} is the delegate setup by {@link #setupOutput()}
                System.out.print("");
            }
        }
    }

    /**
     * The actual {@link Thread} implementation.
     */
    private static final class SlowTaskThreadImpl extends Thread {
        
        private final Runner runner;

        private SlowTaskThreadImpl(BuilderOngoing<?> builder) {
            this(new Runner(builder));
        }

        private SlowTaskThreadImpl(Runner runner) {
            super(runner);
            setDaemon(true);
            this.runner = runner;
        }

        @Override public void interrupt() {
            super.interrupt();
            this.runner.ensureNewLine();
        }       
    }

    /**
     * @param ms the amount of milliseconds to wait until printing a warning
     * @param <T> type of the return result of the {@link Callable}
     * @return the builder for method chaining
     */
    public static <T> BuilderStart<T> after(long ms) {
        if (ms < 1) {
            throw new IllegalArgumentException("Wait duration must be greater than 0.");
        }
        return new BuilderStart<T>(ms);
    }
    
    private SlowTaskThread() { }
    
}
