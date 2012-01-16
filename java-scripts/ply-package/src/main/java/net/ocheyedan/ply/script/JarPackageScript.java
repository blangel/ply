package net.ocheyedan.ply.script;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.PropertiesFileUtil;
import net.ocheyedan.ply.props.Context;
import net.ocheyedan.ply.props.Prop;
import net.ocheyedan.ply.props.Props;
import net.ocheyedan.ply.props.Scope;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * User: blangel
 * Date: 9/24/11
* Time: 1:13 PM
 *
 * Extends {@link ZipPackageScript} to store as a '.jar' file with manifest information.  The properties are specified
 * in the same {@literal package[.scope].properties} file and this script takes into account these additional properties:
 * manifest.version=number [[default=1.0]] (the manifest version number to use).
 * manifest.createdBy=string [[default=ply]] (who created the manifest).
 * manifest.mainClass=string [[default=""]] (the main class to make the jar executable).
 * manifest.classPath=string [[default=""]] (the class path associated with the jar).
 * manifest.spec.title=string [[default=${project[.scope].name}]] (the specification title).
 * manifest.spec.version=string [[default=${project[.scope].version}]] (the specification version).
 * manifest.impl.title=string [[default=${project[.scope].name}]] (the implementation title).
 * manifest.impl.version=string [[default=${project[.scope].version}]] (the implementation title).
 * Additionally, any other project name starting with package[.scope].manifest.* will be included.  For instance,
 * if there is a property=value of manifest.Implementation-Vendor=Ocheyedan in the package[.scope].properties file then
 * there will be an entry in the manifest for 'Implementation-Vendor' with value 'Ocheyedan'.
 * Any manifest property with a null or empty-string value will not be included in the manifest.
 *
 */
public class JarPackageScript extends ZipPackageScript {

    @Override protected String getType() {
        return "jar";
    }

    @Override protected void preprocess() {
        createManifestFile();
        String buildDir = getBuildDir();
        File dependenciesFile = createDependenciesFile(buildDir, new Scope(Props.getValue(Context.named("ply"), "scope")));
        if (dependenciesFile == null) {
            Output.print("^error^ Error creating the %sMETA-INF/ply/dependencies.properties file.", buildDir);
            System.exit(1);
        }
    }

    /**
     * Augment the includes to also have the manifest file and the {@literal project.build.dir}/META-INF/ply files.
     * @param compileDir the directory containing compiled output.
     * @param resourceDir the directory where resources are.
     * @return @see {@link ZipPackageScript#getIncludes(String, String)}
     */
    @Override protected String[] getIncludes(String compileDir, String resourceDir) {
        String[] standardArgs = super.getIncludes(compileDir, resourceDir);

        String manifestFile = getManifestFilePath();
        String buildDir = getBuildDir();

        String[] jarIncludes = new String[standardArgs.length + 4];
        jarIncludes[0] = manifestFile;
        jarIncludes[1] = "-C";
        jarIncludes[2] = buildDir;
        jarIncludes[3] = "META-INF/ply";
        System.arraycopy(standardArgs, 0, jarIncludes, 4, standardArgs.length);

        return jarIncludes;
    }

    @Override protected String getBaseOptions() {
        return "cfm";
    }

    protected String getBuildDir() {
        String buildDir = Props.getValue(Context.named("project"), "build.dir");
        return buildDir + (buildDir.endsWith(File.separator) ? "" : File.separator);
    }

    /**
     * Create the manifest file to be included in the packaged file.
     */
    private void createManifestFile() {
        Map<String, Prop> manifestProps = getManifestProps();
        Context packageContext = Context.named("package");
        // filter out and handle the short-named manifest properties
        String version = Props.getValue(packageContext, "manifest.version");
        manifestProps.remove("manifest.version");
        if (isEmpty(version)) {
            version = "1.0";
        }
        String createdBy = Props.getValue(packageContext, "manfiest.createdBy");
        manifestProps.remove("manfiest.createdBy");
        if (isEmpty(createdBy)) {
            createdBy = "Ply";
        }
        String mainClass = Props.getValue(packageContext, "manifest.mainClass");
        manifestProps.remove("manifest.mainClass");
        String classPath = Props.getValue(packageContext, "manifest.classPath");
        manifestProps.remove("manifest.classPath");
        String specTitle = Props.getValue(packageContext, "manifest.spec.title");
        manifestProps.remove("manifest.spec.title");
        String specVersion = Props.getValue(packageContext, "manifest.spec.version");
        manifestProps.remove("manifest.spec.version");
        String implTitle = Props.getValue(packageContext, "manifest.impl.title");
        manifestProps.remove("manifest.impl.title");
        String implVersion = Props.getValue(packageContext, "manifest.impl.version");
        manifestProps.remove("manifest.impl.version");

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
        for (String property : manifestProps.keySet()) {
            appendManifestInformation(property, manifestProps.get(property).value, buffer);
        }
        File manifestFile = new File(getManifestFilePath());
        PrintWriter writer = null;
        try {
            manifestFile.getParentFile().mkdirs();
            manifestFile.createNewFile();
            writer = new PrintWriter(manifestFile);
            // important, manifest files must end in a new line
            writer.println(buffer.toString());
            writer.flush();
        } catch (IOException ioe) {
            Output.print(ioe);
            ioe.printStackTrace();
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

    /**
     * @return a mapping of property name to {@link Prop} for all properties prefixed with {@literal manifest.} defined
     *         within context {@literal package}
     */
    protected static Map<String, Prop> getManifestProps() {
        Collection<Prop> packageProps = Props.get(Context.named("package"));
        Map<String, Prop> manifestProps = new HashMap<String, Prop>();
        for (Prop prop : packageProps) {
            if (prop.name.startsWith("manifest.")) {
                manifestProps.put(prop.name, prop);
            }
        }
        return manifestProps;
    }

    protected static String getManifestFilePath() {
        String buildDirPath = Props.getValue(Context.named("project"), "build.dir");
        File metaInfDir = FileUtil.fromParts(buildDirPath, "META-INF");
        metaInfDir.mkdirs();
        return FileUtil.pathFromParts(metaInfDir.getPath(), "Manifest.mf");
    }

    /**
     * Reads in the {@literal resolved-deps.properties} file stored at {@literal project[.scope].build.dir} and copies it
     * to {@literal project[.scope].build.dir}/META-INF/ply/dependencies.properties stripping away the property values (as
     * the values are the resolved local-repo paths to the dependencies).  If there is no {@literal resolved-deps.properties}
     * file then a blank file will be copied to {@literal project[.scope].build.dir}/META-INF/ply/dependencies.properties.
     * @param buildDirPath the build directory path (assumed to end in {@link File#separator}).
     * @param scope of the execution
     * @return the handle to the created dependencies.properties file or null if an error occurred while creating the file.
     */
    private static File createDependenciesFile(String buildDirPath, Scope scope) {
        // read in resolved-deps.properties file
        Properties dependencies = new Properties();
        Properties resolvedDeps = PropertiesFileUtil.load(buildDirPath + String.format("resolved-deps%s.properties",
                                                          scope.getFileSuffix()), true);
        for (String propertyName : resolvedDeps.stringPropertyNames()) {
            dependencies.put(propertyName, "");
        }
        File metaInfPlyDepFile = FileUtil.fromParts(buildDirPath, "META-INF", "ply", "dependencies.properties");
        PropertiesFileUtil.store(dependencies, metaInfPlyDepFile.getPath(), true);
        return metaInfPlyDepFile;
    }

}