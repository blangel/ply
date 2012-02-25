package net.ocheyedan.ply.script;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.dep.Deps;
import net.ocheyedan.ply.props.Context;
import net.ocheyedan.ply.props.PropFile;
import net.ocheyedan.ply.props.Props;

import java.io.*;

/**
 * User: blangel
 * Date: 11/17/11
 * Time: 10:16 AM
 *
 * Executes the project provided it has a 'exec.class' or 'package.mainClass' property defined.
 */
public class ExecScript {

    public static void main(String[] args) {
        String mainClass = !Props.get("class", Context.named("exec")).value().isEmpty()
                            ?  Props.get("class", Context.named("exec")).value()
                            : Props.get("manifest.mainClass", Context.named("package")).value();
        if (((mainClass == null) || mainClass.isEmpty())) {
            Output.print("^warn^ Project doesn't have a 'exec.class' or 'package.manifest.mainClass' property set, skipping execution.");
            return;
        }
        String artifactName = Props.get("name", Context.named("package")).value();
        String buildDirPath = Props.get("build.dir", Context.named("project")).value();
        String artifactPath = FileUtil.pathFromParts(buildDirPath, artifactName);
        PropFile deps = Deps.getResolvedProperties(false);
        String classpath = Deps.getClasspath(deps, artifactPath);
        String java = Props.get("java", Context.named("ply")).value();
        String[] javaArgs = new String[4 + args.length];
        javaArgs[0] = java;
        javaArgs[1] = "-cp";
        javaArgs[2] = classpath;
        if (args.length > 0) {
            System.arraycopy(args, 0, javaArgs, 3, args.length);
        }
        javaArgs[javaArgs.length - 1] = mainClass;
        ProcessBuilder processBuilder = new ProcessBuilder(javaArgs).redirectErrorStream(true);
        String outputFilePath = Props.get("output", Context.named("exec")).value();
        PrintStream output;
        try {
            if (outputFilePath.isEmpty() || "stdout".equals(outputFilePath)) {
                output = System.out;
            } else {
                File outputFile = new File(outputFilePath);
                outputFile.createNewFile();
                output = new PrintStream(new FileOutputStream(outputFile));
            }
        } catch (IOException ioe) {
            Output.print("^warn^ Could not access ^b^%s^r^ [ %s ], output is being directed to ^b^stdout^r^.", outputFilePath, ioe.getMessage());
            output = System.out;
        }
        try {
            Output.print("Executing ^b^%s^r^", getExecCommand(javaArgs));
            // the Process thread reaps the child if the parent (this) is terminated
            Process process = processBuilder.start();
            InputStream processStdout = process.getInputStream();
            BufferedReader lineReader = new BufferedReader(new InputStreamReader(processStdout));
            String processStdoutLine;
            while ((processStdoutLine = lineReader.readLine()) != null) {
                output.print(processStdoutLine);
            }
            int result = process.waitFor();
            if (result != 0) {
                System.exit(result);
            }
        } catch (IOException ioe) {
            throw new AssertionError(ioe);
        } catch (InterruptedException ie) {
            throw new AssertionError(ie);
        }
    }

    private static String getExecCommand(String[] args) {
        StringBuilder buffer = new StringBuilder();
        for (String arg : args) {
            if (buffer.length() != 0) {
                buffer.append(' ');
            }
            buffer.append(arg);
        }
        return buffer.toString();
    }

}
