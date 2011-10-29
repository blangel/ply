package org.moxie.ply.script;

import org.moxie.ply.FileUtil;
import org.moxie.ply.Output;
import org.moxie.ply.PropertiesFileUtil;
import org.moxie.ply.props.Prop;
import org.moxie.ply.props.Props;
import org.moxie.ply.props.Scope;
import org.moxie.ply.script.print.PrivilegedOutput;
import org.moxie.ply.script.print.PrivilegedPrintStream;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

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

        Prop buildDirProp = Props.get("project", "build.dir");
        Prop artifactNameProp = Props.get("project", "artifact.name");
        if ((buildDirProp == null) || (artifactNameProp == null)) {
            Output.print("^warn^ No project.build.dir or project.artifact.name found, skipping test execution.");
            return;
        }

        // load the resolved dependencies file from the ply-dependency-manager script
        Scope scope = new Scope(Props.getValue("ply", "scope"));
        String resolvedDepFileName = "resolved-deps" + scope.fileSuffix + ".properties";
        Properties resolvedDepProps = PropertiesFileUtil.load(FileUtil.fromParts(buildDirProp.value, resolvedDepFileName).getPath(),
                                                              false, true);
        if (resolvedDepProps == null) {
            Output.print("^warn^ No %s file found, skipping test execution.", resolvedDepFileName);
            return;
        }

        File artifact = FileUtil.fromParts(buildDirProp.value, artifactNameProp.value);
        if (!artifact.exists()) {
            Output.print("^warn^ No test artifact, skipping test execution.");
            return;
        }
        List<URL> urls = getClasspathEntries(artifact, resolvedDepProps);

        // create a loader with the given test artifact and its dependencies
        ClassLoader loader = URLClassLoader.newInstance(
                urls.toArray(new URL[urls.size()]),
                JunitTester.class.getClassLoader()
        );

        FilenameFilter filter = new FilenameFilter() {
            @Override public boolean accept(File dir, String name) {
                return name.endsWith(".class");
            }
        };
        Set<String> classNames = getClasses(artifact, filter);

        Set<Class> classes = loadClasses(classNames, loader);

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
            Prop reportDirProp = Props.get("project", "reports.dir");
            if (reportDirProp == null) {
                Output.print("^warn^ Could not find property project.reports.dir, skipping out/err redirection.");
                break redirect;
            }
            File outFile = FileUtil.fromParts(reportDirProp.value, "tests-out.txt");
            outFile.getParentFile().mkdirs();
            outFile.createNewFile();
            File errFile = FileUtil.fromParts(reportDirProp.value, "tests-err.txt");
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
            Class junit4RunnerClass = loader.loadClass("org.moxie.ply.script.Junit4Invoker");
            Runnable instance = (Runnable) junit4RunnerClass.getConstructor(Set.class, String[].class,  String.class)
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

    private static List<URL> getClasspathEntries(File artifact, Properties dependencies) {
        List<URL> urls = new ArrayList<URL>();
        URL artifactUrl = getUrl(artifact);
        if (artifactUrl == null) {
            return null;
        }
        urls.add(artifactUrl);
        if (dependencies == null) {
            return urls;
        }

        for (String depName : dependencies.stringPropertyNames()) {
            String depPath = dependencies.getProperty(depName);
            URL depUrl = getUrl(new File(depPath));
            if (depUrl == null) {
                return null;
            }
            urls.add(depUrl);
        }
        return urls;
    }

    private static URL getUrl(File artifact) {
        URL artifactUrl;
        try {
            artifactUrl = new URL("file://" + artifact.getCanonicalPath());
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
                    // ignore
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
