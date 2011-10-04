package org.moxie.ply.script;

import org.moxie.ply.PropertiesUtil;

import java.io.*;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarFile;

/**
 * User: blangel
 * Date: 9/24/11
 * Time: 1:13 PM
 *
 * Packages all files within {@literal compiler.buildPath} into a jar file and stores within {@literal project.build.dir}
 * as {@literal package-jar.jar-name}.jar
 * The property file used to configure this script is {@literal package-jar.properties} and so the context is
 * {@literal package-jar}.
 * The following properties exist:
 * jarName=string [[default=${project.artifact.name}]] (the name of the jar file to create [excluding the '.jar'])
 * verbose=boolean [[default=false]] (print verbose output).
 * compress=boolean [[default=true]] (if true, the jar file will be compressed).
 * manifest.version=number [[default=1.0]] (the manifest version number to use).
 * manifest.createdBy=string [[default=ply]] (who created the manifest).
 * manifest.mainClass=string [[default=""]] (the main class to make the jar executable).
 * manifest.classPath=string [[default=""]] (the class path associated with the jar).
 * manifest.spec.title=string [[default=${project.name}]] (the specification title).
 * manifest.spec.version=string [[default=${project.version}]] (the specification version).
 * manifest.impl.title=string [[default=${project.name}]] (the implementation title).
 * manifest.impl.version=string [[default=${project.version}]] (the implementation title).
 * Additionally, any other project name starting with package-jar.manifest.* will be included.  For instance, if there
 * is a property=value of manifest.Implementation-Vendor=Moxie in the package-jar.properties file then there will be
 * an entry in the manifest for 'Implementation-Vendor' with value 'Moxie'.
 * Any manifest property with a null or empty-string value will not be included in the manifest.
 *
 * TODO
 *   - Handle resources
 */
public class JarPackageScript {

    public static void main(String[] args) {
        JarPackageScript jarPackageScript = new JarPackageScript();
        try {
            jarPackageScript.invoke();
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        } catch (InterruptedException ie) {
            System.out.println(ie.getMessage());
        }
    }

    private void invoke() throws IOException, InterruptedException {
        createManifestFile();
        String[] cmdArgs = createArgs();
        ProcessBuilder processBuilder = new ProcessBuilder(cmdArgs).redirectErrorStream(true);
        Process process = processBuilder.start();
        InputStream processStdout = process.getInputStream();
        BufferedReader lineReader = new BufferedReader(new InputStreamReader(processStdout));
        String processStdoutLine;
        while ((processStdoutLine = lineReader.readLine()) != null) {
            System.out.println(processStdoutLine);
        }
        int result = process.waitFor();
        System.exit(result);
    }

    private void createManifestFile() {
        Set<String> manifestProperties = new HashSet<String>();
        for (String envPropertyName : System.getenv().keySet()) {
            if (envPropertyName.startsWith("package-jar.manifest.")) {
                manifestProperties.add(envPropertyName);
            }
        }
        // filter out and handle the short-named manifest properties
        String version = System.getenv("package-jar.manifest.version");
        manifestProperties.remove("package-jar.manifest.version");
        if (isEmpty(version)) {
            version = "1.0";
        }
        String createdBy = System.getenv("package-jar.manfiest.createdBy");
        manifestProperties.remove("package-jar.manfiest.createdBy");
        if (isEmpty(createdBy)) {
            createdBy = "Ply";
        }
        String mainClass = System.getenv("package-jar.manifest.mainClass");
        manifestProperties.remove("package-jar.manifest.mainClass");
        String classPath = System.getenv("package-jar.manifest.classPath");
        manifestProperties.remove("package-jar.manifest.classPath");
        String specTitle = System.getenv("package-jar.manifest.spec.title");
        manifestProperties.remove("package-jar.manifest.spec.title");
        String specVersion = System.getenv("package-jar.manifest.spec.version");
        manifestProperties.remove("package-jar.manifest.spec.version");
        String implTitle = System.getenv("package-jar.manifest.impl.title");
        manifestProperties.remove("package-jar.manifest.impl.title");
        String implVersion = System.getenv("package-jar.manifest.impl.version");
        manifestProperties.remove("package-jar.manifest.impl.version");

        StringBuilder buffer = new StringBuilder();
        appendManifestInformation("Manifest-Version", version, buffer);
        appendManifestInformation("Created-By", createdBy, buffer);
        appendManifestInformation("Main-Class", mainClass, buffer);
        appendManifestInformation("Class-Path", classPath, buffer);
        appendManifestInformation("Specification-Title", specTitle, buffer);
        appendManifestInformation("Specification-Version", specVersion, buffer);
        appendManifestInformation("Implementation-Title", implTitle, buffer);
        appendManifestInformation("Implementation-Version", implVersion, buffer);

        // add user defined information, if any.
        for (String property : manifestProperties) {
            String name = property.replace("package-jar.manifest.", "");
            appendManifestInformation(name, System.getenv(property), buffer);
        }
        File manifestFile = new File(getManifestFilePath());
        PrintWriter writer = null;
        try {
            manifestFile.createNewFile();
            writer = new PrintWriter(manifestFile);
            // important, manifest files must end in a new line
            writer.println(buffer.toString());
            writer.flush();
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
            System.exit(1);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    private static void appendManifestInformation(String name, String value, StringBuilder buffer) {
        if (!isEmpty(value)) {
            buffer.append(name);
            buffer.append(": ");
            buffer.append(value);
            buffer.append('\n');
        }
    }

    private String[] createArgs() {
        String jarScript = System.getenv("ply.java").replace("bin" + File.separator + "java", "bin" + File.separator + "jar");
        String options = "cfm";
        if (getBoolean(System.getenv("package-jar.verbose"))) {
            options += "v";
        }
        if (!getBoolean(System.getenv("package-jar.compress"))) {
            options += "0";
        }
        String jarName = System.getenv("package-jar.jarName");
        if (isEmpty(jarName)) {
            System.out.println("^warn^ Property 'package-jar.jarName' was empty, defaulting to value of ${project.artifact.name}.");
            jarName = System.getenv("project.artifact.name");
            if (isEmpty(jarName)) {
                System.out.println("^warn^ Property 'project.artifact.name' was empty, defaulting to value of ${project.name}.");
                jarName = System.getenv("project.artifact.name");
                if (isEmpty(jarName)) {
                    System.out.println("^warn^ Property 'project.name' was empty, defaulting to 'no-name'.");
                    jarName = "no-name";
                }
            }
            jarName = jarName + ".jar";
        }
        jarName = getJarFilePath(jarName);
        String manifestFile = getManifestFilePath();
        String inputFiles = System.getenv("compiler.buildPath");

        String buildDir = System.getenv("project.build.dir");
        buildDir = buildDir + (buildDir.endsWith(File.separator) ? "" : File.separator);
        File dependenciesFile = createDependenciesFile(buildDir);
        if (dependenciesFile == null) {
            System.out.printf("^error^ Error creating the %sMETA-INF/ply/dependencies.properties file.\n", buildDir);
            System.exit(1);
        }

        return new String[] { jarScript, options, jarName, manifestFile, "-C", inputFiles, ".", "-C", buildDir, "META-INF/ply" };
    }

    private static String getManifestFilePath() {
        String buildDirPath = System.getenv("project.build.dir");
        File metaInfDir = new File(buildDirPath + (buildDirPath.endsWith(File.separator) ? "" : File.separator) + "META-INF");
        metaInfDir.mkdir();
        return (metaInfDir.getPath() + (metaInfDir.getPath().endsWith(File.separator) ? "" : File.separator) + "Manifest.mf");
    }

    private static String getJarFilePath(String jarName) {
        String buildDirPath = System.getenv("project.build.dir");
        return buildDirPath + (buildDirPath.endsWith(File.separator) ? "" : File.separator) + jarName;
    }

    /**
     * Reads in the {@literal resolved-deps.properties} file stored at {@literal project.build.dir} and copies it
     * to {@literal project.build.dir}/META-INF/ply/dependencies.properties stripping away the property values (as
     * the values are the resolved local-repo paths to the dependencies).  If there is no {@literal resolved-deps.properties}
     * file then a blank file will be copied to {@literal project.build.dir}/META-INF/ply/dependencies.properties.
     * @param buildDirPath the build directory path (assumed to end in {@link File#separator}).
     * @return the handle to the created dependencies.properties file or null if an error occurred while creating the file.
     */
    private static File createDependenciesFile(String buildDirPath) {
        // read in resolved-deps.properties file
        Properties dependencies = new Properties();
        Properties resolvedDeps = PropertiesUtil.load(buildDirPath + "resolved-deps.properties", true);
        for (String propertyName : resolvedDeps.stringPropertyNames()) {
            dependencies.put(propertyName, "");
        }
        File metaInfPlyDepFile = new File(buildDirPath + "META-INF/ply/dependencies.properties");
        PropertiesUtil.store(dependencies, metaInfPlyDepFile.getPath(), true);
        return metaInfPlyDepFile;
    }

    private static boolean isEmpty(String value) {
        return ((value == null) || value.isEmpty());
    }

    private static boolean getBoolean(String value) {
        return ((value != null) && value.equalsIgnoreCase("true"));
    }

}
