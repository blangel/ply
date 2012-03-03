package net.ocheyedan.ply.script;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.dep.Deps;
import net.ocheyedan.ply.props.Context;
import net.ocheyedan.ply.props.PropFile;
import net.ocheyedan.ply.props.PropFileChain;
import net.ocheyedan.ply.props.Props;

import java.io.*;
import java.util.*;

/**
 * User: blangel
 * Date: 11/17/11
 * Time: 10:16 AM
 *
 * Executes the packaged project provided either the property {@literal exec.class} or {@literal package.mainClass}
 * is defined or there are arguments to this script.
 * <p/>
 * If there are any arguments to this script they are taken to be everything except the classpath ({@literal -cp})
 * argument to the java process; i.e., the execution will look like this:
 * <pre>
 *     java -cp {@literal project-classpath} {@literal args}
 * </pre>
 * Otherwise, the main-class is taken from {@literal exec.class} (or {@literal package.mainClass} if it is not defined)
 * and the system arguments are taken from {@literal exec.systemArgx} (where {@literal x} is an increasing positive
 * integer) and the program arguments are taken from {@literal exec.programArgx} (where {@literal x} is an increasing
 * positive integer).
 * For instance, say the following {@literal exec} properties file exists:
 * <pre>
 *     exec.class=net.ocheyedan.ply.Ply
 *     exec.systemArg0=something
 *     exec.systemArg1=somethingelse=test
 *     exec.programArg0=program-arg
 * </pre>
 * The execution would become:
 * <pre>
 *     java -cp {@literal project-classpath} -Dsomething -Dsomethingelse=test net.ocheyedan.ply.Ply program-arg
 * </pre>
 */
public class ExecScript {

    public static void main(String[] args) {
        String[] execArgs;
        if (args.length != 0) {
            execArgs = args;
        } else {
            execArgs = getExecArgs();
        }
        String java = Props.get("java", Context.named("ply")).value();
        String[] javaArgs = new String[3 + execArgs.length];
        javaArgs[0] = java;
        javaArgs[1] = "-cp";
        javaArgs[2] = getClasspath();
        System.arraycopy(execArgs, 0, javaArgs, 3, execArgs.length);
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
                output.println(processStdoutLine);
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
    
    private static String[] getExecArgs() {
        String mainClass = Props.get("class", Context.named("exec")).value();
        if (mainClass.isEmpty()) {
            mainClass = Props.get("manifest.mainClass", Context.named("package")).value();
        }
        if (mainClass.isEmpty()) {
            Output.print("^warn^ Project doesn't have a 'exec.class' or 'package.manifest.mainClass' property set, skipping execution.");
            System.exit(0);
        }
        PropFileChain execProps = Props.get(Context.named("exec"));
        Map<Integer, String> systemArgs = new HashMap<Integer, String>(2);
        Map<Integer, String> programArgs = new HashMap<Integer, String>(2);
        for (PropFile.Prop execProp : execProps.props()) {
            if (execProp.name.startsWith("systemArg")) {
                try {
                    int index = Integer.parseInt(execProp.name.substring(9));
                    if (systemArgs.containsKey(index)) {
                        Output.print("^warn^ Two system arguments with the same index [ %s and %s ] using the second value.", 
                                systemArgs.get(index), execProp.value());
                    }
                    systemArgs.put(index, execProp.value());
                } catch (NumberFormatException nfe) {
                    Output.print("^warn^ Ignoring an invalid system argument [ %s=%s ]. Example of a valid argument: systemArg0=value",
                            execProp.name, execProp.value());
                }
            } else if (execProp.name.startsWith("programArg")) {
                try {
                    int index = Integer.parseInt(execProp.name.substring(10));
                    if (programArgs.containsKey(index)) {
                        Output.print("^warn^ Two program arguments with the same index [ %s and %s ] using the second value.",
                                programArgs.get(index), execProp.value());
                    }
                    programArgs.put(index, execProp.value());
                } catch (NumberFormatException nfe) {
                    Output.print("^warn^ Ignoring an invalid program argument [ %s=%s ]. Example of a valid argument: programArg0=value",
                            execProp.name, execProp.value());
                }
            }
        }
        String[] execArgs = new String[1 + systemArgs.size() + programArgs.size()];
        int index = 0;
        List<Integer> systemArgIndices = new ArrayList<Integer>(systemArgs.keySet());
        Collections.sort(systemArgIndices);
        List<Integer> programArgIndices = new ArrayList<Integer>(programArgs.keySet());
        Collections.sort(programArgIndices);
        if (!systemArgs.isEmpty()) {
            for (Integer systemArgIndex : systemArgIndices) {
                execArgs[index++] = "-D" + systemArgs.get(systemArgIndex);
            }
        }
        execArgs[index++] = mainClass;
        if (!programArgs.isEmpty()) {
            for (Integer programArgIndex : programArgIndices) {
                execArgs[index++] = programArgs.get(programArgIndex);
            }
        }
        return execArgs;
    }
    
    private static String getClasspath() {
        String artifactName = Props.get("name", Context.named("package")).value();
        String buildDirPath = Props.get("build.dir", Context.named("project")).value();
        String artifactPath = FileUtil.pathFromParts(buildDirPath, artifactName);
        PropFile deps = Deps.getResolvedProperties(false);
        return Deps.getClasspath(deps, artifactPath);
    }

}
