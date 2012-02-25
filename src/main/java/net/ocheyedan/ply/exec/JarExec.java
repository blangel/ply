package net.ocheyedan.ply.exec;

import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.SystemExit;
import net.ocheyedan.ply.dep.*;
import net.ocheyedan.ply.graph.DirectedAcyclicGraph;
import net.ocheyedan.ply.props.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static net.ocheyedan.ply.props.PropFile.Prop;

/**
 * User: blangel
 * Date: 10/2/11
 * Time: 7:29 PM
 */
public final class JarExec {

    /**
     * Translates {@code execution#scriptArgs[0]} into an executable statement for a JVM invoker.
     * The whole command array needs to be processed as parameters to the JVM need to be inserted
     * into the command array.
     * @param execution to invoke
     * @param configDirectory the ply configuration directory from which to resolve properties
     * @return the translated execution
     */
    static Execution createJarExecutable(Execution execution, File configDirectory) {
        String classpath = null;
        AtomicReference<String> mainClass = new AtomicReference<String>();
        AtomicBoolean staticClasspath = new AtomicBoolean(false);
        String[] options = getJarScriptOptions(configDirectory, execution, staticClasspath);
        if (!staticClasspath.get()) {
            classpath = getClasspathEntries(execution.executionArgs[0], execution.script.scope, mainClass, configDirectory);
        }
        int classpathLength = (staticClasspath.get() ? 0 : classpath == null ? 2 : 3);
        int propertyLength = 3;
        // add the appropriate java command
        String script = System.getProperty("ply.java");
        String[] newCmdArray = new String[execution.executionArgs.length + options.length + classpathLength + propertyLength];
        newCmdArray[0] = script;
        // pay-forward the -D props passed into ply itself
        newCmdArray[1] = "-Dply.home=" + System.getProperty("ply.home");
        newCmdArray[2] = "-Dply.version=" + System.getProperty("ply.version");
        newCmdArray[3] = "-Dply.java=" + System.getProperty("ply.java");
        System.arraycopy(options, 0, newCmdArray, 4, options.length);
        // if the '-classpath' option is specified, can't use '-jar' option (or rather vice-versa), so
        // need to explicitly give the Main-Class value (implies, using -classpath implicitly via dependencies
        // file means the jar is built with the Main-Class specified).
        if (classpath != null) {
            newCmdArray[options.length + propertyLength + 1] = "-classpath";
            newCmdArray[options.length + propertyLength + 2] = classpath;
            newCmdArray[options.length + propertyLength + 3] = mainClass.get(); // main-class must be found if using implicit dependencies
        } else if (!staticClasspath.get()) {
            newCmdArray[options.length + propertyLength + 1] = "-jar";
            newCmdArray[options.length + propertyLength + 2] = execution.executionArgs[0];
        }
        System.arraycopy(execution.executionArgs, 1, newCmdArray, options.length + classpathLength + propertyLength + 1,
                execution.executionArgs.length - 1);
        return execution.with(newCmdArray);
    }

    /**
     * Constructs a classpath element for {@code jarPath} (including it itself, {@code jarPath}, on the path) by
     * analyzing the jar at {@code jarPath} for a {@literal META-INF/ply/dependencies.properties} file within it.
     * If one is found, its values are resolved and returned as the classpath (with the appropriate path separator),
     * @param jarPath of the jar to get classpath entries
     * @param scope of the execution
     * @param mainClass will be set with the 'Main-Class' value within the jar, if it is present
     * @param projectConfigDir the ply configuration directory from which to resolve properties
     * @return the classpath (including the given {@code jarPath}).
     */
    private static String getClasspathEntries(String jarPath, Scope scope, AtomicReference<String> mainClass,
                                              File projectConfigDir) {
        JarFile jarFile = null;
        try {
            jarFile = new JarFile(jarPath, false);
            Manifest manifest = jarFile.getManifest();
            if (manifest != null) {
                mainClass.set(manifest.getMainAttributes().getValue("Main-Class"));
            }
            JarEntry dependenciesJarEntry = jarFile.getJarEntry("META-INF/ply/dependencies.properties");
            if (dependenciesJarEntry == null) {
                return null;
            }
            InputStream dependenciesStream = jarFile.getInputStream(dependenciesJarEntry);
            PropFile dependencies = new PropFile(Context.named("dependencies"), PropFile.Loc.Local);
            PropFileReader.Default.load(new BufferedReader(new InputStreamReader(dependenciesStream)), dependencies);
            // if there are no dependencies, the 'dependencies.properties' may still exist, just empty; so ignore
            if (dependencies.isEmpty()) {
                return null;
            }
            List<DependencyAtom> deps = Deps.parse(dependencies);
            RepositoryRegistry repos = createRepositoryList(projectConfigDir, scope);
            DirectedAcyclicGraph<Dep> depGraph = Deps.getDependencyGraph(deps, repos);
            PropFile resolvedDependencies = Deps.convertToResolvedPropertiesFile(depGraph);
            return Deps.getClasspath(resolvedDependencies, jarPath);
        } catch (IOException ioe) {
            Output.print(ioe);
            throw new SystemExit(1);
        } finally {
            if (jarFile != null) {
                try {
                    jarFile.close(); // note, closes entry's input-streams as well
                } catch (IOException ioe) {
                    throw new AssertionError(ioe);
                }
            }
        }
    }

    private static RepositoryRegistry createRepositoryList(File configDirectory, Scope scope) {
        String localRepoValue = Props.get("localRepo", Context.named("depmngr"), scope, configDirectory).value();
        RepositoryAtom localRepo = RepositoryAtom.parse(localRepoValue.isEmpty() ? null : localRepoValue);
        if (localRepo == null) {
            Output.print("^error^ No ^b^localRepo^r^ property defined (^b^ply set localRepo=xxxx in depmngr^r^).");
            throw new SystemExit(1);
        }
        List<RepositoryAtom> repositoryAtoms = new ArrayList<RepositoryAtom>();
        PropFileChain repositoryProps = Props.get(Context.named("repositories"), scope, configDirectory);
        for (Prop repositoryProp : repositoryProps.props()) {
            if (localRepo.getPropertyName().equals(repositoryProp.name)) {
                continue;
            }
            String repoType = repositoryProp.value();
            String repoAtom = repoType + ":" + repositoryProp.name;
            RepositoryAtom repo = RepositoryAtom.parse(repoAtom);
            if (repo == null) {
                Output.print("^warn^ Invalid repository declared %s, ignoring.", repoAtom);
            } else {
                repositoryAtoms.add(repo);
            }
        }
        Collections.sort(repositoryAtoms, RepositoryAtom.LOCAL_COMPARATOR);
        return new RepositoryRegistry(localRepo, repositoryAtoms, null);
    }

    /**
     * Retrieves the jvm options for {@code execution} or the default options if none have been specified.
     * @param configDirectory the ply configuration directory from which to resolve properties
     * @param execution for which to retrieve options
     * @param staticClasspath will be set by this method to true iff the resolved options contains a
     *        {@literal -classpath} or {@literal -cp} value.
     * @return the split jvm options for {@code script}
     */
    private static String[] getJarScriptOptions(File configDirectory, Execution execution, AtomicBoolean staticClasspath) {
        String executable = execution.executionArgs[0];
        // strip the resolved path (just use the jar name)
        int index = executable.lastIndexOf(File.separator);
        if (index != -1) {
            executable = executable.substring(index + 1);
        }
        String options = Props.get("options." + executable, Context.named("scripts-jar"), execution.script.scope, configDirectory).value();
        if (options.isEmpty()) {
            options = Props.get("options.default", Context.named("scripts-jar"), execution.script.scope, configDirectory).value();
        }
        if (options.contains("-cp") || options.contains("-classpath")) {
            staticClasspath.set(true);
        }
        return options.split(" ");
    }

    private JarExec() { }

}