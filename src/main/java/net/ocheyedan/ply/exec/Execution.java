package net.ocheyedan.ply.exec;

import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.OutputExt;
import net.ocheyedan.ply.PwdUtil;
import net.ocheyedan.ply.cmd.build.Script;
import net.ocheyedan.ply.props.Context;

import java.io.*;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * User: blangel
 * Date: 11/13/11
 * Time: 2:02 PM
 *
 * Represents a concrete execution.  Unlike, {@link net.ocheyedan.ply.cmd.build.Script}, this class represents something
 * which can be executed via a {@link Process}.  To make a {@link net.ocheyedan.ply.cmd.build.Script} executable it is
 * necessary to resolve its invoking mechanism; say direct, via a JVM, etc.
 */
public class Execution {

    /**
     * A thread-safe pipe to be used as the {@literal stdin} for the {@link Process} created to invoke this execution.
     */
    protected static final ThreadLocal<StdinProcessPipe> STDIN_PROCESS_PIPE = new ThreadLocal<StdinProcessPipe>() {
        @Override protected StdinProcessPipe initialValue() {
            return new StdinProcessPipe();
        }
    };

    /**
     * A name to use when identifying this execution.
     */
    public final String name;

    /**
     * The underlying script for this execution.
     */
    public final Script script;

    /**
     * The arguments to the execution for the script.  By convention of {@link Process} the
     * first item is the execution itself (i.e., 'java').  So for an execution without args this
     * array has one value, the execution itself.
     */
    public final String[] executionArgs;

    /**
     * The {@link ProcessBuilder} which will ultimately creates the {@link Process}.
     * This is typically built within {@link #preInvoke(java.io.File, java.util.Map)}
     */
    protected final AtomicReference<ProcessBuilder> processBuilder;

    /**
     * The associated {@link Process} object's stdout.
     */
    protected final AtomicReference<BufferedReader> processStdout;

    /**
     * The actual {@link Process} created when invoking this execution.  By default, this is set when {@link #invoke(String)}
     * is executed.
     */
    protected final AtomicReference<Process> process;

    public Execution(String name, Script script, String[] executionArgs) {
        this.name = name;
        this.script = script;
        this.executionArgs = executionArgs;
        this.processBuilder = new AtomicReference<ProcessBuilder>();
        this.processStdout = new AtomicReference<BufferedReader>();
        this.process = new AtomicReference<Process>();
    }

    public Execution augment(String[] with) {
        String[] args = new String[this.executionArgs.length + with.length];
        System.arraycopy(this.executionArgs, 0, args, 0, this.executionArgs.length);
        System.arraycopy(with, 0, args, this.executionArgs.length, with.length);
        return new Execution(name, script, args);
    }

    public Execution with(String executable) {
        String[] args = new String[this.executionArgs.length];
        System.arraycopy(this.executionArgs, 1, args, 1, this.executionArgs.length - 1);
        args[0] = executable;
        return new Execution(name, this.script, args);
    }

    /**
     * Constructs an appropriate environment variable name for the given execution.
     * @param prefix to prepend to the variable name to distinguish this variable as a ply variable
     * @param context of the property
     * @param propName of the property
     * @return the execution specific environment variable name
     */
    public String getEnvKey(String prefix, Context context, String propName) {
        return prefix + context.name + "." + propName;
    }

    /**
     * @return an id for the type of environment key; override this method when overriding
     *         {@link #getEnvKey(String, net.ocheyedan.ply.props.Context, String)} to allow for proper caching
     *         of resolved environment keys.
     */
    public String getEnvKeyId() {
        return "execution";
    }

    /**
     * Allows executions the ability to startup and then pause. If an implementation allows for this then they will pause
     * after starting up and then wait until {@link #invoke(String)} is called.
     * This functionality is useful for slow-starting executions like {@link JvmExecution} where the time to start
     * a {@literal JVM} is long and so this 'start-up' time can be threaded by {@link Exec} to allow for faster
     * execution without sacrificing process isolation (i.e., running the {@literal JVM} scripts in the same
     * process).
     *
     * @param projectRoot for which to set the root directory for the invoked process
     * @param supplementalEnvironment environment variables to pass to the invoked process
     */
    void preInvoke(File projectRoot, Map<String, String> supplementalEnvironment) {
        ProcessBuilder processBuilder = new ProcessBuilder(executionArgs).redirectErrorStream(true).directory(projectRoot);
        Map<String, String> environment = processBuilder.environment();
        environment.putAll(supplementalEnvironment);
        this.processBuilder.set(processBuilder);
    }

    /**
     * Invokes the execution. Or in the case of execution implementations which implement
     * {@link #preInvoke(java.io.File, java.util.Map)}, starts the process.
     * @param scriptName the name of the execution
     */
    void invoke(String scriptName) throws IOException {
        Output.print("^dbug^ invoking %s", scriptName);
        // the Process thread reaps the child if the parent (this) is terminated
        final Process process = processBuilder.get().start();
        this.process.set(process);

        // take the parent's input and pipe to the child's output
        STDIN_PROCESS_PIPE.get().startPipe(process.getOutputStream());
        // capture the child's input for output on parent process
        this.processStdout.set(new BufferedReader(new InputStreamReader(process.getInputStream())));
    }

    /**
     * Waits for the execution to complete.
     * @return the process exit code
     * @see Process#waitFor()
     */
    int waitFor(String outputScriptName) throws IOException, InterruptedException {
        // take the child's input and reformat for output on parent process
        String processStdoutLine;
        while ((processStdoutLine = processStdout.get().readLine()) != null) {
            PwdUtil.Request request = PwdUtil.isPwdRequest(processStdoutLine);  // determine if the line is a password request
            OutputExt.printFromExec("[^green^%s^r^] %s", outputScriptName, request.getLine());
            if (!request.isPwd()) {
                continue;
            }
            // child-process is requesting a password-read; handle via {@link System#console()} if available
            Console console = System.console();
            char[] pwd;
            if (console != null) {
                pwd = console.readPassword();
            } else { // no console available, simply read (potentially with echo-on)
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                pwd = reader.readLine().toCharArray();
            }
            STDIN_PROCESS_PIPE.get().write(pwd);
            Arrays.fill(pwd, ' ');
        }
        int result = process.get().waitFor();
        STDIN_PROCESS_PIPE.get().pausePipe();
        return result;
    }

    /**
     * Kills the associated {@link Process} if any.
     */
    void kill() {
        if (process.get() != null) {
            process.get().destroy();
        }
    }

    @Override public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Execution execution = (Execution) o;

        if (script != null ? !script.equals(execution.script) : execution.script != null) {
            return false;
        }
        return Arrays.equals(executionArgs, execution.executionArgs);
    }

    @Override public int hashCode() {
        int result = script != null ? script.hashCode() : 0;
        result = 31 * result + Arrays.hashCode(executionArgs);
        return result;
    }
}
