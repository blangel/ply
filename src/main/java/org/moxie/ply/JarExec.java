package org.moxie.ply;

import org.moxie.ply.dep.DependencyAtom;
import org.moxie.ply.dep.Deps;
import org.moxie.ply.dep.RepositoryAtom;
import org.moxie.ply.props.Prop;
import org.moxie.ply.props.Props;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * User: blangel
 * Date: 10/2/11
 * Time: 7:29 PM
 */
public class JarExec {

    /**
     * Translates {@code cmdArray[0]} into an executable statement for a JVM invoker.
     * The whole command array needs to be processed as parameters to the JVM need to be inserted
     * into the command array.
     * @param unresolvedScript the unresolved script name (i.e., with path information).
     * @param cmdArray to translate
     * @return the translated command array.
     */
    public static String[] createJarExecutable(String unresolvedScript, String[] cmdArray) {
        String script = cmdArray[0];
        String classpath = null;
        AtomicReference<String> mainClass = new AtomicReference<String>();
        AtomicBoolean staticClasspath = new AtomicBoolean(false);
        String[] options = getJarScriptOptions(unresolvedScript, staticClasspath);
        if (!staticClasspath.get()) {
            classpath = getClasspathEntries(script, mainClass);
        }
        int classpathLength = (staticClasspath.get() ? 0 : classpath == null ? 2 : 3);
        // add the appropriate java command
        script = System.getProperty("ply.java");
        String[] newCmdArray = new String[cmdArray.length + options.length + classpathLength];
        newCmdArray[0] = script;
        System.arraycopy(options, 0, newCmdArray, 1, options.length);
        // if the '-classpath' option is specified, can't use '-jar' option (or rather vice-versa), so
        // need to explicitly give the Main-Class value (implies, using -classpath implicitly via dependencies
        // file means the jar is built with the Main-Class specified).
        if (classpath != null) {
            newCmdArray[options.length + 1] = "-classpath";
            newCmdArray[options.length + 2] = classpath;
            newCmdArray[options.length + 3] = mainClass.get(); // main-class must be found if using implicit dependencies
        } else if (!staticClasspath.get()) {
            newCmdArray[options.length + 1] = "-jar";
            newCmdArray[options.length + 2] = cmdArray[0];
        }
        System.arraycopy(cmdArray, 1, newCmdArray, options.length + classpathLength + 1, cmdArray.length - 1);
        return newCmdArray;
    }

    /**
     * Constructs a classpath element for {@code jarPath} (including it itself, {@code jarPath}, on the path) by
     * analyzing the jar at {@code jarPath} for a {@literal META-INF/ply/dependencies.properties} file within it.
     * If one is found, its values are resolved and returned as the classpath (with the appropriate path separator),
     * @param jarPath of the jar to get classpath entries
     * @param mainClass will be set with the 'Main-Class' value within the jar, if it is present
     * @return the classpath (including the given {@code jarPath}).
     */
    private static String getClasspathEntries(String jarPath, AtomicReference<String> mainClass) {
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
            Properties dependencies = new Properties();
            dependencies.load(dependenciesStream);
            // if there are no dependencies, the 'dependencies.properties' may still exist, just empty; so ignore
            if (dependencies.isEmpty()) {
                return null;
            }
            List<DependencyAtom> dependencyAtoms = new ArrayList<DependencyAtom>(dependencies.size());
            AtomicReference<String> error = new AtomicReference<String>();
            for (String dependencyName : dependencies.stringPropertyNames()) {
                error.set(null);
                DependencyAtom dependencyAtom = DependencyAtom.parse(dependencyName, error);
                if (dependencyAtom == null) {
                    Output.print("^warn^ could not parse dependency ^b^%s^r^, ignoring.", dependencyName);
                } else {
                    dependencyAtoms.add(dependencyAtom);
                }
            }
            Properties resolvedDependencies = Deps
                    .resolveDependencies(dependencyAtoms, createRepositoryList());
            StringBuilder classpath = new StringBuilder();
            for (String resolvedDependency : resolvedDependencies.stringPropertyNames()) {
                classpath.append(resolvedDependencies.getProperty(resolvedDependency));
                classpath.append(File.pathSeparator);
            }
            classpath.append(jarPath);
            return classpath.toString();
        } catch (IOException ioe) {
            Output.print(ioe);
            System.exit(1);
        } finally {
            if (jarFile != null) {
                try {
                    jarFile.close(); // note, closes entry's input-streams as well
                } catch (IOException ioe) {
                    // ignore
                }
            }
        }
        return null;
    }

    private static List<RepositoryAtom> createRepositoryList() {
        String filteredLocalRepo = Props.filter(Props.get("depmngr", "localRepo"));
        RepositoryAtom localRepo = RepositoryAtom.parse(filteredLocalRepo);
        if (localRepo == null) {
            Output.print("^error^ Local repository not defined.  Set 'localRepo' property in context 'depmngr'");
            System.exit(1);
        }
        List<RepositoryAtom> repositoryAtoms = new ArrayList<RepositoryAtom>();
        repositoryAtoms.add(localRepo);
        Map<String, Prop> repositoryProps = Props.getProperties("repositories", Props.DEFAULT_SCOPE);
        if (repositoryProps != null) {
            for (String repoUri : repositoryProps.keySet()) {
                if (localRepo.getPropertyName().equals(repoUri)) {
                    continue;
                }
                String repoType = repositoryProps.get(repoUri).value;
                String repoAtom = repoUri + "::" + repoType;
                RepositoryAtom repo = RepositoryAtom.parse(repoAtom);
                if (repo == null) {
                    Output.print("^warn^ Invalid repository declared %s, ignoring.", repoAtom);
                } else {
                    repositoryAtoms.add(repo);
                }
            }
        }
        return repositoryAtoms;
    }

    /**
     * Retrieves the jvm options for {@code script} or the default options if none have been specified.
     * @param script for which to find options
     * @param staticClasspath will be set by this method to true iff the resolved options contains a
     *        {@literal -classpath} or {@literal -cp} value.
     * @return the split jvm options for {@code script}
     */
    private static String[] getJarScriptOptions(String script, AtomicBoolean staticClasspath) {
        String options = Props.getValue("scripts-jar", Props.DEFAULT_SCOPE, "options." + script);
        if (options.isEmpty()) {
            options = Props.getValue("scripts-jar", Props.DEFAULT_SCOPE, "options.default");
        }
        options = Props.filter(new Prop("scripts-jar", "", "", options, true));
        if (options.contains("-cp") || options.contains("-classpath")) {
            staticClasspath.set(true);
        }
        return options.split(" ");
    }

}
