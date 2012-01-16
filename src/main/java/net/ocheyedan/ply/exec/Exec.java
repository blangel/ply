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

/**
 * User: blangel
 * Date: 9/3/11
 * Time: 9:09 PM
 *
 * Invokes {@link Execution} objects.
 */
public final class Exec {

    private final static StdinProcessPipe STDIN_PROCESS_PIPE = new StdinProcessPipe();

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
        for (Execution execution : executions) {
            if (!invoke(execution, projectRoot)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Invokes {@code execution} and routes all output to this process's output stream.
     * @param execution to invoke
     * @param projectRoot for which to set the root directory for the process handling the {@code execution}
     * @return false if the invocation of {@code execution} failed for any reason.
     */
    private static boolean invoke(Execution execution, File projectRoot) {
        File projectConfigDir = FileUtil.fromParts(projectRoot.getPath(), ".ply", "config");
        execution = handleNonNativeExecutable(execution, projectConfigDir);
        String script = Output.isDebug() ? buildScriptName(execution.executionArgs) : "";
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(execution.executionArgs).redirectErrorStream(true).directory(projectRoot);

            Map<String, String> environment = processBuilder.environment();
            environment.putAll(PropsExt.getPropsForEnv(projectConfigDir, execution.script.scope));

            Output.print("^dbug^ invoking %s", script);
            // the Process thread reaps the child if the parent (this) is terminated
            final Process process = processBuilder.start();
            // take the parent's input and pipe to the child's output
            STDIN_PROCESS_PIPE.startPipe(process.getOutputStream());
            // take the child's input and reformat for output on parent process
            BufferedReader processStdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String processStdoutLine;
            while ((processStdoutLine = processStdout.readLine()) != null) {
                OutputExt.printFromExec("[^green^%s^r^] %s", buildExecutionName(execution), processStdoutLine);
            }
            int result = process.waitFor();
            STDIN_PROCESS_PIPE.pausePipe();
            if (result == 0) {
                return true;
            }
            Output.print("^error^ script ^green^%s^r^ failed [ exit code = %d ].", execution.script.unparsedName, result);
        } catch (IOException ioe) {
            Output.print("^error^ executing script ^green^%s^r^", execution.script.unparsedName);
            Output.print(ioe);
        } catch (InterruptedException ie) {
            Output.print(ie);
        }
        return false;
    }

    private static String buildExecutionName(Execution execution) {
        String name = execution.name;
        String scope = execution.script.scope.getScriptPrefix();
        // only prefix with scope if the execution name isn't the same as the scope
        if (!name.equals(execution.script.scope.name)) {
            return scope + name;
        } else {
            return name;
        }
    }

    private static String buildScriptName(String[] cmdArgs) {
        StringBuilder buffer = new StringBuilder();
        for (String cmdArg : cmdArgs) {
            buffer.append(cmdArg);
            buffer.append(" ");
        }
        buffer.replace(buffer.length() - 1, buffer.length(), "");
        return buffer.toString();
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
        if (execution.script instanceof ShellScript) {
            return ShellExec.createShellExecutable(execution, configDirectory);
        } else if (executable.endsWith(".jar")) {
            return JarExec.createJarExecutable(execution, configDirectory);
        } else if (executable.endsWith(".clj")) {
            return ClojureExec.createClojureExecutable(execution, configDirectory);
        }
        return execution;
    }

}