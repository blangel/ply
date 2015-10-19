package net.ocheyedan.ply.script;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.dep.DependencyAtom;
import net.ocheyedan.ply.dep.Deps;
import net.ocheyedan.ply.jna.JnaAccessor;
import net.ocheyedan.ply.props.Context;
import net.ocheyedan.ply.props.PropFile;
import net.ocheyedan.ply.props.Props;

import java.io.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

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
 * includeDeps=boolean [[default=false]] (if true, the dependencies will be included in the archive).
 */
public class ZipPackageScript implements PackagingScript {

    @Override public void invoke() throws IOException, InterruptedException {
        String buildPath = Props.get("build.path", Context.named("compiler")).value();
        String resBuildPath = Props.get("res.build.dir", Context.named("project")).value();
        File buildPathDir = new File(buildPath);
        File resBuildPathDir = new File(resBuildPath);
        if (!buildPathDir.exists() && !resBuildPathDir.exists()) {
            Output.print("Nothing to package, skipping.");
            System.exit(0);
        }
        preprocess();
        String[] cmdArgs = createArgs(getType(), null, getIncludes(buildPath, resBuildPath));
        Output.print("^dbug^ Creating package with arguments: %s", Arrays.toString(cmdArgs));
        ProcessBuilder processBuilder = new ProcessBuilder(cmdArgs).redirectErrorStream(true);
        Process process;
        try {
            process = processBuilder.start();
        } catch (IOException ioe) {
            Output.print("^error^ Error creating %s file %s", getType(), Arrays.toString(cmdArgs));
            throw ioe;
        }
        InputStream processStdout = process.getInputStream();
        BufferedReader lineReader = new BufferedReader(new InputStreamReader(processStdout));
        String processStdoutLine;
        while ((processStdoutLine = lineReader.readLine()) != null) {
            System.out.println(processStdoutLine); // don't use Output, just print directly and let ply itself handle
        }
        int result = process.waitFor();
        if (result != 0) {
            System.exit(result);
        } else {
            result = postprocess(result);
        }
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
     * Allows subclasses a hook to process after the package is created (provided the creation succeeded).
     * @param exitCode exit code of the package creation
     * @return an exit code (which should be {@code exitCode} unless an exception occurs).
     */
    protected int postprocess(int exitCode) throws IOException, InterruptedException {
        exitCode |= packageSources();
        if (getBoolean(Props.get("includeDeps", Context.named("package")).value())) {
            PropFile deps = Deps.getResolvedProperties(false);
            if (deps.isEmpty()) {
                return exitCode;
            }
            String packaging = getType();
            int numberDeps = deps.size();
            Output.print("^info^ Including ^b^%d^r^ dependenc%s in ^b^%s^r^ package.", numberDeps,
                         (numberDeps == 1 ? "y" : "ies"), packaging);
            String name = getPackageName(packaging, null);
            String nameWithDeps = getPackageName(packaging, "with-deps");
            File nameWithDepsFile = new File(nameWithDeps);
            ZipOutputStream output = null;
            try {
                nameWithDepsFile.createNewFile();
                Set<String> existing = new HashSet<String>();
                output = new ZipOutputStream(new FileOutputStream(nameWithDepsFile));
                ZipFiles.append(new ZipInputStream(new FileInputStream(name)), output, existing);
                for (PropFile.Prop dep : deps.props()) {
                    if (DependencyAtom.isTransient(dep.name)) {
                        continue;
                    }
                    String depFile = dep.value();
                    ZipFiles.append(new ZipInputStream(new FileInputStream(depFile)), output, existing);
                }
                output.close();
            } catch (FileNotFoundException fnfe) {
                throw new AssertionError(fnfe);
            } catch (IOException ioe) {
                Output.print(ioe);
                return 1;
            } finally {
                if (output != null) {
                    try {
                        output.close();
                    } catch (IOException ioe) {
                        throw new AssertionError(ioe);
                    }
                }
            }
        }
        return exitCode;
    }

    protected int packageSources() throws IOException, InterruptedException {
        if (getBoolean(Props.get("includeSrc", Context.named("package")).value())) {
            Context projectContext = Context.named("project");
            String srcDirPath = Props.get("src.dir", projectContext).value();
            String resDirPath = Props.get("res.dir", projectContext).value();
            String[] cmdArgs = createArgs(getType(), "sources", getIncludes(srcDirPath, resDirPath));
            Output.print("^dbug^ Creating source package with arguments: %s", Arrays.toString(cmdArgs));
            ProcessBuilder processBuilder = new ProcessBuilder(cmdArgs).redirectErrorStream(true);
            Process process;
            try {
                process = processBuilder.start();
            } catch (IOException ioe) {
                Output.print("^error^ Error creating %s file %s", getType(), Arrays.toString(cmdArgs));
                throw ioe;
            }
            InputStream processStdout = process.getInputStream();
            BufferedReader lineReader = new BufferedReader(new InputStreamReader(processStdout));
            String processStdoutLine;
            while ((processStdoutLine = lineReader.readLine()) != null) {
                System.out.println(processStdoutLine); // don't use Output, just print directly and let ply itself handle
            }
            return process.waitFor();
        }
        return 0;
    }

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
            if (subdirectoriesIntersect(compileDirFile, resourceDirFile)) {
                return getIntersectedDirectories(compileDirFile, compileDir, resourceDirFile);
            } else {
                return new String[] { "-C", compileDir, ".", "-C", resourceDir, "." };
            }
        } else if (compileDirFile.exists()) {
            return new String[] { "-C", compileDir, "." };
        } else if (resourceDirFile.exists()) {
            return new String[] { "-C", resourceDir, "." };
        }
        return new String[0];
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
     * @param packagingNameSuffix suffix for the package (i.e., sources)
     * @param includes list of directories/files to include within the package (may include '-C' information)
     * @return the arguments to the {@literal jar} executable.
     */
    protected String[] createArgs(String packaging, String packagingNameSuffix, String[] includes) {
        String jarScript = Props.get("java", Context.named("ply")).value().replace("bin" + File.separator + "java",
                "bin" + File.separator + "jar");
        String options = getBaseOptions();
        if (getBoolean(Props.get("verbose", Context.named("package")).value())) {
            options += "v";
        }
        if (!getBoolean(Props.get("compress", Context.named("package")).value())) {
            options += "0";
        }
        String name = getPackageName(packaging, packagingNameSuffix);

        String[] args = new String[includes.length + 3];
        args[0] = jarScript;
        args[1] = options;
        args[2] = name;
        System.arraycopy(includes, 0, args, 3, includes.length);
        return args;
    }

    protected String getPackageName(String packaging, String suffix) {
        String name = Props.get("name", Context.named("package")).value();
        if (name.isEmpty()) {
            Output.print("^warn^ Property 'package.name' was empty, defaulting to value of ${project.artifact.name}.");
            name = Props.get("artifact.name", Context.named("project")).value();
            if (name.isEmpty()) {
                Output.print("^warn^ Property 'project.artifact.name' was empty, defaulting to value of ${project.name}.");
                name = Props.get("name", Context.named("project")).value();
                if (name.isEmpty()) {
                    Output.print("^warn^ Property 'project.name' was empty, defaulting to 'no-name'.");
                    name = "no-name";
                }
            }
            name = name + "." + packaging;
        }
        if ((suffix != null) && !suffix.isEmpty()) {
            if (name.lastIndexOf(".") != -1) {
                name = name.substring(0, name.lastIndexOf(".")) + "-" + suffix + name.substring(name.lastIndexOf("."));
            } else {
                name = name + "-" + suffix;
            }
        }
        return getPackageFilePath(name);
    }

    static String getPackageFilePath(String name) {
        String buildDirPath = Props.get("build.dir", Context.named("project")).value();
        return FileUtil.pathFromParts(buildDirPath, name);
    }

    static boolean getBoolean(String value) {
        return "true".equalsIgnoreCase(value);
    }

    /**
     * A {@link FilenameFilter} which only accepts directories.
     */
    private static final FilenameFilter DIRECTORY_FILTER = new FilenameFilter() {
        @Override public boolean accept(File dir, String name) {
            return (FileUtil.fromParts(dir.getPath(), name)).isDirectory();
        }
    };

    /**
     * @param first directory to compare to {@code second}
     * @param second directory to compare with {@code first}
     * @return true if {@code first} and {@code second} have overlapping sub-directory structures.
     */
    static boolean subdirectoriesIntersect(File first, File second) {
        String[] firstSubfileNames = first.list(DIRECTORY_FILTER);
        String[] secondSubfileNames = second.list(DIRECTORY_FILTER);
        Arrays.sort(firstSubfileNames);
        Arrays.sort(secondSubfileNames);
        for (String firstSubfileName : firstSubfileNames) {
            if (Arrays.binarySearch(secondSubfileNames, firstSubfileName) >= 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param first into which to link files from {@code second}
     * @param firstPath the pre-computed path of {@code first}
     * @param second from which to link all files into {@code first}
     * @return jar command arguments for {@code first} (note, all files within {@code second} are first linked into {@code first}).
     */
    static String[] getIntersectedDirectories(File first, String firstPath, File second) {
        // TODO - either do programmatically (create package via java-api not jar command or at least
        // TODO - link the files into an independent directory so as to not pollute classes/resources dirs).
        linkIntersectedDirectories(first, second);
        return new String[] { "-C", firstPath, "." };
    }

    /**
     * Links all files within {@code from} to {@code to}
     * @param to directory into which to link all files within {@code from}
     * @param from directory from which to link all files into {@code to}
     */
    private static void linkIntersectedDirectories(File to, File from) {
        for (File secondFile : from.listFiles()) {
            File firstFile = FileUtil.fromParts(to.getPath(), secondFile.getName());
            if (firstFile.exists()) {
                if (secondFile.isDirectory()) {
                    linkIntersectedDirectories(firstFile, secondFile);
                } else {
                    Output.print("^warn^ Found a duplicate entry [ ^b^%s^r^ ], skipping inclusion of ^i^^yellow^%s^r^.", firstFile.getPath(), secondFile.getPath());
                }
            } else {
                if (JnaAccessor.getCUnixLibrary() != null) {
                    JnaAccessor.getCUnixLibrary().symlink(secondFile.getAbsolutePath(), firstFile.getAbsolutePath());
                } else {
                    FileUtil.copy(secondFile, firstFile);
                }
            }
        }
    }

}
