package org.moxie.ply.script;

import org.moxie.ply.Output;
import org.moxie.ply.props.Prop;
import org.moxie.ply.props.Props;

import javax.xml.transform.Result;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;
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
 * The set of {@link Class} objects to search comes from the {@literal project#test#build.dir}/{@literal project#test#artifact.name}.
 * If no such artifact exists, this script does nothing.
 */
public class JunitTester {

    public static void main(String[] args) {

        Prop testBuildDirProp = Props.get("project", "test", "build.dir");
        Prop testArtifactNameProp = Props.get("project", "test", "artifact.name");
        if ((testBuildDirProp == null) || (testArtifactNameProp == null)) {
            Output.print("^warn^ No project#test#build.dir or project#test#artifact.name found, skipping test execution.");
            return;
        }
        File artifact = new File(testBuildDirProp.value + File.separator + testArtifactNameProp.value);
        if (!artifact.exists()) {
            Output.print("^info^ No test artifact, skipping test execution.");
            return;
        }
        URL artifactUrl;
        try {
            artifactUrl = new URL("file://" + artifact.getCanonicalPath());
        } catch (MalformedURLException murle) {
            Output.print(murle);
            return;
        } catch (IOException ioe) {
            Output.print(ioe);
            return;
        }
        // create a loader with the given test artifact
        ClassLoader loader = URLClassLoader.newInstance(
                new URL[] { artifactUrl },
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

        Junit4Runner junit4Runner = new Junit4Runner(classes, matchers, unsplitMatchers);
        junit4Runner.runTests();

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
