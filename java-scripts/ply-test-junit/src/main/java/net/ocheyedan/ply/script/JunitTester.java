package net.ocheyedan.ply.script;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.PlyUtil;
import net.ocheyedan.ply.dep.Deps;
import net.ocheyedan.ply.dep.RepositoryAtom;
import net.ocheyedan.ply.input.Resources;
import net.ocheyedan.ply.props.Context;
import net.ocheyedan.ply.props.PropFile;
import net.ocheyedan.ply.props.Props;
import net.ocheyedan.ply.script.print.PrivilegedOutput;
import net.ocheyedan.ply.script.print.PrivilegedPrintStream;

import java.io.*;
import java.lang.String;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import static net.ocheyedan.ply.props.PropFile.Prop;

/**
 * User: blangel
 * Date: 10/13/11
 * Time: 8:30 AM
 *
 * Executes {@literal junit} test cases for all classes within the project's test artifact or those classes which
 * match the first argument to this script's main method.  The argument is a comma delimited list of test-atoms where
 * a test-atom is:
 * class[#method]
 * {@literal class} represents a {@link Class} and the optional {@literal method} represents a method name on that
 * {@literal class}.  Both the {@literal class} and {@literal method} variables can be an {@literal ant} style
 * wildcards.  The wildcard applied to the {@literal class} variable means to search matching packages and {@link Class}
 * names and the wildcard applied to the {@literal method} variable means to search matching method names on the
 * matched {@link Class} object(s).
 * The set of {@link Class} objects to search comes from the {@literal project.scope.build.dir}/{@literal project.scope.artifact.name}.
 * If no such artifact exists, this script does nothing.
 *
 */
public class JunitTester {

    @SuppressWarnings("unchecked") /* ignore unchecked in constructor call */
    public static void main(String[] args) {

        Prop buildDirProp = Props.get("build.dir", Context.named("project"));
        Prop artifactNameProp = Props.get("artifact.name", Context.named("project"));
        if (Prop.Empty.equals(buildDirProp) || Prop.Empty.equals(artifactNameProp)) {
            Output.print("^warn^ No project.build.dir or project.artifact.name found, skipping test execution.");
            return;
        }

        // load the resolved dependencies file from the ply-dependency-manager script
        PropFile resolvedDepProps = Deps.getResolvedProperties(true);
        if (resolvedDepProps == null) {
            Output.print("^warn^ No resolved-deps.properties file found, skipping test execution.");
            return;
        }

        File artifact = FileUtil.fromParts(buildDirProp.value(), artifactNameProp.value());
        if (!artifact.exists()) {
            Output.print("^warn^ No test artifact, skipping test execution.");
            return;
        }
        List<URL> urls = getClasspathEntries(artifact, resolvedDepProps);

        // create a loader with the given test artifact and its dependencies
        ClassLoader loader = URLClassLoader.newInstance(
                urls.toArray(new URL[urls.size()]),
                // use the boot class-loader so as to not interfere with any common jars shared by this tester and the
                // artifact being tested
                null
        );

        FilenameFilter filter = new FilenameFilter() {
            @Override public boolean accept(File dir, String name) {
                return name.endsWith(".class");
            }
        };
        Set<String> classNames = getClasses(artifact, filter);
        Set<Class> classes = loadClasses(classNames, loader);
        Output.print("^dbug^ Loaded %d classes from test artifact.", classes.size());

        String[] matchers = null;
        String unsplitMatchers = null;
        if (args.length == 1) {
            unsplitMatchers = args[0];
            matchers = args[0].split(",");
        }

        // redirect out/err to a log file (except privileged code from this package)
        PrintStream oldOut = System.out;
        PrintStream oldErr = System.err;
        redirect:try {
            // create the redirected out/err files.
            Prop reportDirProp = Props.get("reports.dir", Context.named("project"));
            if (Prop.Empty.equals(reportDirProp)) {
                Output.print("^warn^ Could not find property project.reports.dir, skipping out/err redirection.");
                break redirect;
            }
            File outFile = FileUtil.fromParts(reportDirProp.value(), "tests-out.txt");
            outFile.getParentFile().mkdirs();
            outFile.createNewFile();
            File errFile = FileUtil.fromParts(reportDirProp.value(), "tests-err.txt");
            errFile.createNewFile();
            System.setOut(new PrivilegedPrintStream(oldOut, outFile));
            System.setErr(new PrivilegedPrintStream(oldErr, errFile));
        } catch (IOException ioe) {
            PrivilegedOutput.print(ioe);
            System.exit(1);
        }

        // invoke the Junit4Runner in a thread to force usage of the {@code loader} which has reference to the
        // resolved dependencies
        try {
            Resources.setResourcesLoader(loader);
            Class junit4Invoker = loader.loadClass("net.ocheyedan.ply.script.Junit4Invoker");
            Runnable instance = (Runnable) junit4Invoker.getConstructor(Set.class, String[].class,  String.class)
                                                 .newInstance(classes, matchers, unsplitMatchers);
            Thread runner = new Thread(instance);
            runner.setContextClassLoader(loader);
            runner.start();
            runner.join();
        } catch (ClassNotFoundException cfne) {
            PrivilegedOutput.print(cfne);
            System.exit(1);
        } catch (NoSuchMethodException nsme) {
            throw new AssertionError(nsme);
        } catch (InstantiationException ie) {
            PrivilegedOutput.print(ie.getCause());
            System.exit(1);
        } catch (IllegalAccessException iae) {
            throw new AssertionError(iae);
        } catch (InvocationTargetException ite) {
            PrivilegedOutput.print(ite.getCause());
            System.exit(1);
        } catch (InterruptedException ie) {
            PrivilegedOutput.print(ie);
            System.exit(1);
        }

    }

    private static List<URL> getClasspathEntries(File artifact, PropFile dependencies) {
        List<URL> urls = new ArrayList<URL>();
        URL artifactUrl = getUrl(artifact);
        if (artifactUrl == null) {
            throw new AssertionError(String.format("Could not find artifact: %s", artifact.getPath()));
        }
        urls.add(artifactUrl);
        if (dependencies == null) {
            return urls;
        }

        boolean includesPlyUtil = false;
        for (Prop depName : dependencies.props()) {
            // TODO - should this exclude the direct-transient deps? perhaps not b/c need for testing?
            String depPath = depName.value();
            URL depUrl = getUrl(new File(depPath));
            if (depUrl == null) {
                throw new AssertionError(String.format("Could not find dependency artifact: %s", depPath));
            }
            urls.add(depUrl);
            // TODO - is this the best way to handle those projects which depend upon ply-util?
            if (depName.name.contains("ply-util")) {
                includesPlyUtil = true;
            }
        }

        // now add our own dependencies
        PropFile.Prop localRepoProp = Props.get("localRepo", Context.named("depmngr"));
        RepositoryAtom localRepo = RepositoryAtom.parse(localRepoProp.value());
        if (localRepo == null) {
            if (PropFile.Prop.Empty.equals(localRepoProp)) {
                Output.print("^error^ No ^b^localRepo^r^ property defined (^b^ply set localRepo=xxxx in depmngr^r^).");
            } else {
                Output.print("^error^ Could not resolve directory for ^b^localRepo^r^ property [ is ^b^%s^r^ ].", localRepoProp.value());
            }
            System.exit(1);
        }
        String localRepoDirectoryPath = Deps.getDirectoryPathForRepo(localRepo);
        // TODO - how to resolve own namespace/name/version and dependencies
        if (!includesPlyUtil) {
            URL plyUtil = getUrl(FileUtil.fromParts(PlyUtil.INSTALL_DIRECTORY, "lib", "ply-util-1.0.jar"));
            if (plyUtil == null) {
                throw new AssertionError("Could not find ^b^ply-util-1.0.jar^r^ in the installation lib directory.");
            }
            urls.add(plyUtil);
        }
        URL plyJunitTester = getUrl(FileUtil.fromParts(PlyUtil.INSTALL_DIRECTORY, "scripts", "ply-test-junit-1.0.jar"));
        if (plyJunitTester == null) {
            throw new AssertionError("Could not find ^b^ply-test-junit-1.0.jar^r^ in the installation scripts directory.");
        }
        urls.add(plyJunitTester);
        URL hamcrest = getUrl(FileUtil.fromParts(localRepoDirectoryPath, "org.hamcrest", "hamcrest-core", "1.1", "hamcrest-core-1.1.jar"));
        if (hamcrest == null) {
            throw new AssertionError("Could not find ^b^hamcrest-core-1.1.jar^r^ in local repository.");
        }
        urls.add(hamcrest);
        URL commonsLang = getUrl(FileUtil.fromParts(localRepoDirectoryPath, "commons-lang", "commons-lang", "2.6", "commons-lang-2.6.jar"));
        if (commonsLang == null) {
            throw new AssertionError("Could not find ^b^commons-lang-2.6.jar^r^ in local repository.");
        }
        urls.add(commonsLang);
        URL junit = getUrl(FileUtil.fromParts(localRepoDirectoryPath, "junit", "junit", "4.10", "junit-4.10.jar"));
        if (junit == null) {
            throw new AssertionError("Could not find ^b^junit-4.10.jar^r^ in local repository.");
        }
        urls.add(junit);

        return urls;
    }

    private static URL getUrl(File artifact) {
        URL artifactUrl;
        try {
            String path = artifact.getPath();
            if (path.startsWith("file://")) {
                path = path.substring(7);
            } else if (path.startsWith("file:/")) {
                path = path.substring(6);
            } else if (path.startsWith("file:")) {
                path = path.substring(5);
            }
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            artifactUrl = new URL("file://" + path);
        } catch (MalformedURLException murle) {
            Output.print(murle);
            return null;
        } catch (IOException ioe) {
            Output.print(ioe);
            return null;
        }
        return artifactUrl;
    }

    private static Set<String> getClasses(File artifact, FilenameFilter filter) {
        Set<String> classes = new HashSet<String>();
        JarInputStream inputStream = null;
        try {
            inputStream = new JarInputStream(new FileInputStream(artifact));
            JarEntry entry = inputStream.getNextJarEntry();
            while (entry != null) {
                if (filter.accept(null, entry.getName())) {
                    String javaClassName = entry.getName().substring(0, entry.getName().length() - 6);
                    javaClassName = javaClassName.replaceAll("/", ".");
                    classes.add(javaClassName);
                }
                entry = inputStream.getNextJarEntry();
            }
        } catch (IOException ioe) {
            Output.print(ioe);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ioe) {
                    throw new AssertionError(ioe);
                }
            }
        }
        return classes;
    }

    private static Set<Class> loadClasses(Set<String> classNames, ClassLoader loader) {
        Set<Class> classes = new HashSet<Class>(classNames.size());
        for (String className : classNames) {
            try {
                Class clazz = Class.forName(className, true, loader);
                classes.add(clazz);
            } catch (ClassNotFoundException cnfe) {
                Output.print(cnfe);
            }
        }
        return classes;
    }

}
