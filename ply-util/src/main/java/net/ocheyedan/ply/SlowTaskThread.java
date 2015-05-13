package net.ocheyedan.ply;

import net.ocheyedan.ply.input.InterruptibleInputReader;
import net.ocheyedan.ply.props.Context;
import net.ocheyedan.ply.props.PropFile;

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
 * Some tasks can be slow (e.g., downloading a large dependency graph).  These tasks can make it seem that ply is
 * hung when in fact it is working, its just that no log messages are being printed.  For such tasks, this
 * thread can be invoked where a warning appears to the user after a set number of seconds to alert the user of
 * what is happening and, optionally, ask the user if it wants to enable more logging.
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
                        PlyUtil.addInvocationProperties("ply.log.levels", PlyUtil.varargs("warn"), "true");
                        Output.enableWarn(); break;
                    case Info:
                        PlyUtil.addInvocationProperties("ply.log.levels", PlyUtil.varargs("info"), "true");
                        Output.enableInfo(); break;
                    case Debug:
                        PlyUtil.addInvocationProperties("ply.log.levels", PlyUtil.varargs("debug"), "true");
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
         * @throws Exception from issuing {@link Callable#call()}
         */
        public T start() throws Exception {
            Thread slowTaskThread = null;
            boolean alreadyAnswered = PlyUtil.matchingInvocationProperty("slowthread",
                    getAlreadyAnsweredKey(builder.logging.get(), builder.warning), "true");
            if (!alreadyAnswered
                    && !builder.logging.get().isLoggingEnabled()
                    // if not ignoring headless (so both headless and not headless are valid) or if not headless
                    && (!builder.ignoreIfHeadless.get() || !PlyUtil.isHeadless())) {
                slowTaskThread = new SlowTaskThreadImpl(builder);
                slowTaskThread.start();
            }
            T result = null;
            try {
                result = task.call();
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

        private final String message;

        private final long wait;
        
        private final String logMessage;

        private final BuilderOngoing.Logging logging;

        private final String alreadyAnswered;

        Runner(BuilderOngoing<?> builder) {
            this.message = builder.warning;
            this.wait = builder.builder.ms;
            this.logMessage = builder.logging.get().message;
            this.logging = builder.logging.get();
            this.alreadyAnswered = getAlreadyAnsweredKey(this.logging, this.message);
        }

        @Override public void run() {
            final boolean invokedByPly = "ply".equals(System.getenv("ply_ply.invoker"));
            try {
                Thread.sleep(wait);
                if (!Thread.currentThread().isInterrupted()) {
                    Output.print(message);
                    final AtomicBoolean printedNewLine = new AtomicBoolean(false); // @see {@link #setupOutput()}
                    if (PlyUtil.isHeadless()) {
                        Output.print(logMessage);
                    } else {
                        if (invokedByPly) {
                            // need to go directly to stdout to avoid Output parsing prior to Exec handling
                            System.out.println(String.format("^no_line^%s Enable now? [Y/n] ", logMessage));
                        } else {
                            Output.printNoLine("%s Enable now? [Y/n] ", logMessage);
                        }
                        // cursor's hanging on the last line of output, if more output comes need to prefix with newline
                        setupOutput(printedNewLine, invokedByPly);
                    }
                    // io-read doesn't interrupt, so need to continuously poll availability of bytes on the stdin
                    // socket via InterruptibleInputReader.
                    if (!PlyUtil.isHeadless()) {
                        InterruptibleInputReader reader = new InterruptibleInputReader(System.in);
                        try {
                            String answer = reader.readLine();
                            printedNewLine.set(true); // the user's response echoes a newline
                            if (answer != null) {
                                if ("y".equalsIgnoreCase(answer.trim())
                                        || "yes".equalsIgnoreCase(answer.trim())) {
                                    logging.enableLogging();
                                }
                                PlyUtil.addInvocationProperties("slowthread", PlyUtil.varargs(alreadyAnswered), "true");
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
         * Causes first output to be printed on a newline.
         * @param printedNewLine true if a new line has already been printed after ply's question to the user (this
         *                       would happen either by the user answering the question or within the delegate below).
         * @param invokedByPly true to indicate the execution is being invoked by ply (false implies this is ply itself)
         * @return the current {@link System#out} at time of this call.
         */
        private PrintStream setupOutput(final AtomicBoolean printedNewLine, final boolean invokedByPly) {
            final PrintStream old = System.out;
            PrintStream tabbed = new PrintStream(new ByteArrayOutputStream() /* spurious as calls are delegated to 'old' */) {
                final Object[] nil = new Object[0];
                @Override public void print(String out) {
                    if (printedNewLine.getAndSet(true)) {
                        old.print(out);
                    } else {
                        if (invokedByPly) {
                            old.print(String.format("^no_prefix^%n%s", out));
                        } else {
                            Output.printNoLine(String.format("%n%s", out));
                        }
                        System.setOut(old);
                    }
                }
                @Override public void println(String out) {
                    if (printedNewLine.getAndSet(true)) {
                        old.println(out);
                    } else {
                        if (invokedByPly) {
                            old.println(String.format("^no_prefix^%n%s", out));
                        } else {
                            Output.print(String.format("%n%s", out));
                        }
                        System.setOut(old);
                    }
                }
            };
            System.setOut(tabbed);
            return old;
        }

        /**
         * Ensures a new line has been printed.  There might not be a new line if the user hasn't responded to
         * the question asked by ply about enabling more log levels.
         */
        private void ensureNewLine() {
            // either the delegate hasn't been called in which case the empty-string print will result in
            // the delegate printing '^no_prefix^%n' or that has already happened and so this is basically a no-op
            System.out.print("");
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

    private static String getAlreadyAnsweredKey(BuilderOngoing.Logging logging, String message) {
        return String.format("%s%s", logging.name(), message);
    }

    private SlowTaskThread() { }

}
