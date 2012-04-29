package net.ocheyedan.ply.exec;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.OutputExt;
import net.ocheyedan.ply.cmd.build.ShellScript;
import net.ocheyedan.ply.props.PropsExt;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * User: blangel
 * Date: 9/3/11
 * Time: 9:09 PM
 *
 * Invokes {@link Execution} objects.
 */
public final class Exec {

    /**
     * Invokes all {@code executions}.
     * @param projectPlyDir the {@literal .ply} directory of the project to invoke
     * @param executions to invoke
     * @return false if any of the invocations of the resolved {@link Execution} objects failed for any reason
     */
    public static boolean invoke(File projectPlyDir, List<Execution> executions) {
        // all invoked scripts will be started from the parent of the '.ply' directory.
        // this provides a consistent view of execution for all scripts.  if a script wants to actually know
        // which directory from which the 'ply' command was invoked, look at 'original.user.dir' environment property.
        File projectRoot = FileUtil.fromParts(projectPlyDir.getPath(), "..");
        // track the running and queued callbacks
        ExecutionWrapper running = null;
        ExecutionWrapper queued = null;
        for (Execution execution : executions) {
            // wait for the running task, if any
            if (!waitFor(running, queued)) {
                return false;
            }
            // the running task has now completed, invoke the queued task
            running = invoke(queued);
            // create a new queued task
            queued = preInvoke(execution, projectRoot);
        }
        // finish up the running/queued processes
        if (!waitFor(running, queued)) {
            return false;
        }
        running = invoke(queued);
        return waitFor(running, null);
    }

    private static ExecutionWrapper preInvoke(Execution execution, File projectRoot) {
        File projectConfigDir = FileUtil.fromParts(projectRoot.getPath(), ".ply", "config");
        execution = handleNonNativeExecutable(execution, projectConfigDir);
        long start = System.currentTimeMillis();
        execution.preInvoke(projectRoot, PropsExt.getPropsForEnv(projectConfigDir, execution.script.scope));
        return new ExecutionWrapper(execution, start);
    }

    private static ExecutionWrapper invoke(ExecutionWrapper queued) {
        if (queued != null) {
            queued.invoke();
        }
        return queued;
    }

    private static boolean waitFor(ExecutionWrapper running, ExecutionWrapper queued) {
        if (!((running == null) || running.waitFor())) {
            if (queued != null) {
                queued.execution.kill();
            }
            return false;
        }
        return true;
    }

    /**
     * Translates {@code execution#scriptArgs} into an executable statement if it needs an invoker like a shell or VM.
     * The whole command array needs to be processed as parameters to the shell/VM may need to be inserted
     * into the command array.
     * @param execution to invoke
     * @param configDirectory the ply configuration directory from which to resolve properties
     * @return the translated execution.
     */
    private static Execution handleNonNativeExecutable(Execution execution, File configDirectory) {
        String executable = execution.executionArgs[0];
        if ((execution.script instanceof ShellScript) || executable.endsWith(".sh")) {
            return ShellExecution.createShellExecutable(execution, configDirectory);
        } else if (executable.endsWith(".jar")) {
            return JvmExecution.createJarExecutable(execution, configDirectory);
        } else if (executable.endsWith(".clj")) {
            return JvmExecution.createClojureExecutable(execution, configDirectory);
        }
        return execution;
    }

}