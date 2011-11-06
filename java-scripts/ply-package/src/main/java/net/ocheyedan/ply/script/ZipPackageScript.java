package net.ocheyedan.ply.script;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.props.Props;

import java.io.*;
import java.util.Arrays;

/**
 * User: blangel
 * Date: 11/5/11
 * Time: 1:54 PM
 *
 * Packages all files within {@literal compiler[.scope].build.path} and {@literal project[.scope].res.build.dir} into a
 * zip file and stores within {@literal project[.scope].build.dir} as {@literal package[.scope].name}.zip
 * The property file used to configure this script is {@literal package[.scope].properties} and so the context is
 * {@literal package}.
 * The following properties exist:
 * name=string [[default=${project[.scope].artifact.name}]] (the name of the package file to create [excluding the packaging; i.e., '.zip'])
 * verbose=boolean [[default=false]] (print verbose output).
 * compress=boolean [[default=true]] (if true, the package file will be compressed).
 */
public class ZipPackageScript implements PackagingScript {

    @Override public void invoke() throws IOException, InterruptedException {
        String buildPath = Props.getValue("compiler", "build.path");
        String resBuildPath = Props.getValue("project", "res.build.dir");
        File buildPathDir = new File(buildPath);
        File resBuildPathDir = new File(resBuildPath);
        if (!buildPathDir.exists() && !resBuildPathDir.exists()) {
            Output.print("Nothing to package, skipping.");
            System.exit(0);
        }
        preprocess();
        String[] cmdArgs = createArgs(getType(), getIncludes(buildPath, resBuildPath));
        ProcessBuilder processBuilder = new ProcessBuilder(cmdArgs).redirectErrorStream(true);
        Process process = processBuilder.start();
        InputStream processStdout = process.getInputStream();
        BufferedReader lineReader = new BufferedReader(new InputStreamReader(processStdout));
        String processStdoutLine;
        while ((processStdoutLine = lineReader.readLine()) != null) {
            System.out.println(processStdoutLine); // don't use Output, just print directly and let ply itself handle
        }
        int result = process.waitFor();
        System.exit(result);
    }

    /**
     * @return the file type
     */
    protected String getType() {
        return "zip";
    }

    /**
     * Allows subclasses a hook to process (create manifest files, etc) before creating arguments and
     * packaging the files.
     */
    protected void preprocess() { }

    /**
     * Allows subclasses a hook to augment the included files/directories within the package.
     * One of {@code compileDir} or {@code resourceDir} must exist.
     * @param compileDir the directory containing compiled output.
     * @param resourceDir the directory where resources are.
     * @return the included files for packaging within the {@literal jar} executable format (i.e., may include '-C' for
     *         directories)
     */
    protected String[] getIncludes(String compileDir, String resourceDir) {
        File compileDirFile = new File(compileDir);
        File resourceDirFile = new File(resourceDir);
        if (compileDirFile.exists() && resourceDirFile.exists()) {
            return new String[] { "-C", compileDir, ".", "-C", resourceDir, "." };
        } else if (compileDirFile.exists()) {
            return new String[] { "-C", compileDir, "." };
        } else {
            return new String[] { "-C", resourceDir, "." };
        }
    }

    /**
     * @return the base options to use when creating the package file.
     * @see {@literal http://download.oracle.com/javase/tutorial/deployment/jar/build.html}
     */
    protected String getBaseOptions() {
        return "cfM";
    }

    /**
     * Creates the arguments to give to the {@literal jar} executable
     * @param packaging type of the project (i.e., zip, jar, war).
     * @param includes list of directories/files to include within the package (may include '-C' information)
     * @return the arguments to the {@literal jar} executable.
     */
    protected String[] createArgs(String packaging, String[] includes) {
        String jarScript = Props.getValue("java").replace("bin" + File.separator + "java",
                "bin" + File.separator + "jar");
        String options = getBaseOptions();
        if (getBoolean(Props.getValue("package", "verbose"))) {
            options += "v";
        }
        if (!getBoolean(Props.getValue("package", "compress"))) {
            options += "0";
        }
        String name = Props.getValue("package", "name");
        if (isEmpty(name)) {
            Output.print("^warn^ Property 'package.name' was empty, defaulting to value of ${project.artifact.name}.");
            name = Props.getValue("project", "artifact.name");
            if (isEmpty(name)) {
                Output.print("^warn^ Property 'project.artifact.name' was empty, defaulting to value of ${project.name}.");
                name = Props.getValue("project", "name");
                if (isEmpty(name)) {
                    Output.print("^warn^ Property 'project.name' was empty, defaulting to 'no-name'.");
                    name = "no-name";
                }
            }
            name = name + "." + packaging;
        }
        name = getPackageFilePath(name);

        String[] args = new String[includes.length + 3];
        args[0] = jarScript;
        args[1] = options;
        args[2] = name;
        System.arraycopy(includes, 0, args, 3, includes.length);
        return args;
    }

    static String getPackageFilePath(String name) {
        String buildDirPath = Props.getValue("project", "build.dir");
        return FileUtil.pathFromParts(buildDirPath, name);
    }

    static boolean isEmpty(String value) {
        return ((value == null) || value.isEmpty());
    }

    static boolean getBoolean(String value) {
        return ((value != null) && value.equalsIgnoreCase("true"));
    }

}
