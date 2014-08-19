package net.ocheyedan.ply.dep;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.SystemExit;
import net.ocheyedan.ply.dep.visitors.DependencyVisitor;
import net.ocheyedan.ply.props.Context;
import net.ocheyedan.ply.props.PropFile;
import net.ocheyedan.ply.props.PropFiles;
import net.ocheyedan.ply.props.Props;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User: blangel
 * Date: 8/18/14
 * Time: 12:23 PM
 *
 * Utility class to build a graph of class dependencies (i.e., Class A depends upon Class B, etc).
 * A tree is built of class dependencies.  The structure is such that any node of a higher depth is depended upon
 * by nodes above it.  This means that if a node changes, only the nodes above it in terms of depth need to be
 * considered for re-compilation/etc.  For instance, given this dependency tree:
 *               o
 *              / \
 *             1  2
 *            / \
 *           3   4
 * If 'o' changes, nothing but itself needs to be recompiled
 * If '1' changes, it and 'o' need to be recompiled
 * If '3' changes, it and '1' and 'o' need to be recompiled
 */
public class ClassDeps {

    /**
     * Loads all class files within {@literal compiler.build.path} (for the current Scope)
     * and generates a dependency graph (according to the comments outlined above for this class).
     * The dependencies for compilation (those nodes above the class node) are stored in directory
     * {@literal compiler.class.deps} (for the current Scope) with the filename equalling the class name.
     */
    public void processClassDependencies() {
        String classPath = Props.get("build.path", Context.named("compiler")).value();
        File classPathFile = new File(classPath);
        Set<String> classes = collectClasses(classPathFile);
        Map<String, Set<String>> dependencies = collectDependencies(classPath, classes);
        String classDepsPath = Props.get("class.deps", Context.named("compiler")).value();
        File classDepsDirectory = new File(classDepsPath);
        if (!classDepsDirectory.exists()) {
            if (!classDepsDirectory.mkdirs()) {
                Output.print("^error^Could not create directory ^b^%s^r^", classDepsPath);
                System.exit(1);
            }
        }
        // see comment below regarding context; this value is irrelevant
        Context context = new Context("classdeps");
        for (Map.Entry<String, Set<String>> entry : dependencies.entrySet()) {
            String className = entry.getKey();
            // leveraging PropFile for it's supporting classes for writing to file;
            // the Context/Loc are irrelevant for this usage
            PropFile propFile = new PropFile(context, PropFile.Loc.AdHoc);
            for (String dependency : entry.getValue()) {
                propFile.add(dependency, "");
            }
            File location = FileUtil.fromParts(classDepsPath, className);
            String fileName = String.format("%s.properties", location.getAbsolutePath());
            PropFiles.store(propFile, fileName, true);
        }
    }

    /**
     * @param classPath directory for which to search for {@literal .class} files
     * @return paths to all {@literal .class} files within directory {@code classPath}
     */
    private Set<String> collectClasses(File classPath) {
        File[] subfiles = classPath.listFiles();
        if (subfiles == null) {
            return Collections.emptySet();
        }
        Set<String> classes = new HashSet<String>();
        for (File file : subfiles) {
            if (file.isDirectory()) {
                classes.addAll(collectClasses(file));
            } else {
                classes.add(file.getAbsolutePath());
            }
        }
        return classes;
    }

    /**
     * @param classBaseDir the base directory for {@code filePaths}
     * @param filePaths set of paths for which to collect dependency information
     * @return a mapping from class name to all other classes which depend upon it. These are the nodes above
     *         the class name node in the graph detailed in the documentation to this class.  The significance of
     *         this mapping is that if the class name (the key in the map) changes then all the values mapped
     *         to the key need to also be recompiled.
     */
    public Map<String, Set<String>> collectDependencies(String classBaseDir, Set<String> filePaths) {
        Map<String, String> files = new ConcurrentHashMap<String, String>(filePaths.size(), 1.0f);
        for (String filePath : filePaths) {
            if (!filePath.endsWith(".class")) {
                Output.print("^warn^Given non-class file for dependency capture [ ^b^%s^r^ ]", filePath);
                continue;
            }
            int pathStart = filePath.indexOf(classBaseDir);
            String filePackageClassName = filePath.substring(pathStart + classBaseDir.length());
            int start = (filePackageClassName.startsWith(File.separator) ? 1 : 0);
            String packageClassName = filePackageClassName.substring(start, (filePackageClassName.length() - 6)).replace(File.separatorChar, '.');
            files.put(packageClassName, filePath);
        }
        Map<String, Set<String>> dependencies = new ConcurrentHashMap<String, Set<String>>(files.size(), 1.0f);
        collectDependencies(files, dependencies);
        return dependencies;
    }

    /**
     * @param files mapping from class name to absolute file path for the associated {@literal .class} file
     * @param dependencies a mapping into which "inverted" dependencies for each class name within {@code files}
     *                     will be placed. This is a mapping from class name to those classes which depend
     *                     upon class name.
     */
    private void collectDependencies(Map<String, String> files, Map<String, Set<String>> dependencies) {
        for (Map.Entry<String, String> entry : files.entrySet()) {
            collectDependencies(entry.getKey(), new File(entry.getValue()), dependencies);
        }
    }

    /**
     * @param className name of the class for which to find the "inverted" dependencies
     * @param classFile file for which to find the "inverted" dependencies
     * @param inverted mapping from a class to all those other classes which depend upon it. This is not a mapping
     *                 from class name to its dependencies, hence the inverted name.
     */
    private void collectDependencies(String className, File classFile, Map<String, Set<String>> inverted) {
        Set<String> dependencies = getDependencies(className, classFile);
        for (String dependency : dependencies) {
            Set<String> invertedDependencies = inverted.get(dependency);
            if (invertedDependencies == null) {
                invertedDependencies = new HashSet<String>();
                inverted.put(dependency, invertedDependencies);
            }
            invertedDependencies.add(className);
        }
    }

    /**
     * @param className for which to find its dependencies
     * @param classFile file to {@code className}
     * @return a set of class names for which {@code className} depends upon. These are {@code className}'s direct dependencies
     */
    public Set<String> getDependencies(String className, File classFile) {
        FileInputStream inputStream;
        try {
            inputStream = new FileInputStream(classFile);
        } catch (IOException ioe) {
            Output.print(ioe);
            SystemExit.exit(1);
            return Collections.emptySet();
        }
        BufferedInputStream buffer = new BufferedInputStream(inputStream);
        DependencyVisitor visitor = new DependencyVisitor(className);
        visitor.visit(buffer);
        return visitor.getDependencies();
    }

}
