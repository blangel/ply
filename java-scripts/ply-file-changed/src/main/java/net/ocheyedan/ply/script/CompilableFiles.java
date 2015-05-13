package net.ocheyedan.ply.script;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.dep.ClassDeps;
import net.ocheyedan.ply.props.*;

import java.io.File;
import java.util.Set;

/**
 * User: blangel
 * Date: 8/19/14
 * Time: 4:35 PM
 */
class CompilableFiles {

    /**
     * Inspects {@code changedFiles} and consults the class-deps graph to determine all files
     * which need to be compiled based on those changed.
     * This also includes any changes necessary because of dependency changes and existing
     * compiler errors.
     * @param changedFiles files known to have been changed
     * @param scope the current scope
     * @param srcDirPath the source directory path
     * @param buildDirPath the build directory path
     * @return a list of all files which need to be compiled because of {@code changedFiles} as well as existing
     *         compiler errors and dependency changes (i.e., upgrade to new version of a dependent jar)
     */
    public PropFile compute(PropFile changedFiles, Scope scope, String srcDirPath, String buildDirPath) {
        String classDepsPath = Props.get("class.deps", Context.named("compiler")).value();
        File classDepsDirectory = new File(classDepsPath);
        if (!classDepsDirectory.exists()) {
            // balk - nothing created in terms of class deps, only those changed files and existing errors can be compiled
            addExistingErrors(buildDirPath, scope, changedFiles);
            return changedFiles;
        }

        // add changed files and any file depending upon the changed file
        Context inconsequential = Context.named("compile");
        PropFile needingCompiling = new PropFile(inconsequential, PropFile.Loc.AdHoc);
        for (PropFile.Prop prop : changedFiles.props()) {
            needingCompiling.add(prop.name, "");
            int index = prop.name.indexOf(srcDirPath);
            String name = prop.name.substring(index + srcDirPath.length());
            String fileName = name.replace(File.separatorChar, '.').replace(".java", ".properties");
            addDependentClasses(classDepsDirectory, fileName, srcDirPath, needingCompiling);
        }

        // if in test-scope, need to add default-scope-compiled dependent files
        if (!Scope.Default.equals(scope) && "test".equals(scope.name)) {
            File compiledSinceTest = FileUtil.fromParts(buildDirPath, "default-scope-compiled.properties");
            PropFile compiledSinceTestProps = PropFiles.load(compiledSinceTest.getAbsolutePath(), false, false);
            for (PropFile.Prop compiledSinceTestProp : compiledSinceTestProps.props()) {
                String defaultSrcDirPath = compiledSinceTestProp.value();
                int index = compiledSinceTestProp.name.indexOf(defaultSrcDirPath);
                String name = compiledSinceTestProp.name.substring(index + defaultSrcDirPath.length());
                String fileName = name.replace(File.separatorChar, '.').replace(".java", ".properties");
                addDependentClasses(classDepsDirectory, fileName, srcDirPath, needingCompiling);
            }
        }

        // for any changed-dep-jar, find any file depending upon files within the jar and add
        File changedDepsFile = FileUtil.fromParts(buildDirPath, "changed-deps" + scope.getFileSuffix() + ".properties");
        if (changedDepsFile.exists()) {
            PropFile changedDeps = PropFiles.load(changedDepsFile.getPath(), false, false);
            for (PropFile.Prop changedDep : changedDeps.props()) {
                String jarFile = changedDep.value();
                ClassDeps classDeps = new ClassDeps();
                Set<String> classesWithinJar = classDeps.getClasses(jarFile);
                for (String classWithinJar : classesWithinJar) {
                    addDependentClasses(classDepsDirectory, classWithinJar, srcDirPath, needingCompiling);
                }
            }
        }

        // add existing errors
        addExistingErrors(buildDirPath, scope, needingCompiling);

        return needingCompiling;
    }

    private void addDependentClasses(File classDepsDirectory, String fileName, String srcDirPath, PropFile propFile) {
        if ((fileName.startsWith(".") || fileName.startsWith(File.separator))) {
            fileName = fileName.substring(1);
        }
        File classDep = FileUtil.fromParts(classDepsDirectory.getAbsolutePath(), fileName);
        if (!classDep.exists()) {
            return;
        }
        // load the class's dependent classes and mark those as needing compiling
        PropFile dependentClasses = PropFiles.load(classDep.getPath(), false, false);
        for (PropFile.Prop dependentClass : dependentClasses.props()) {
            propFile.add(getSourceFile(dependentClass, srcDirPath), "");
        }
    }

    private String getSourceFile(PropFile.Prop dependentClass, String sourceDir) {
        File dependentClassSourcePath;
        // if the dependent class is an inner class (contains $ in name) then the container class
        // of the inner class needs to be compiled (as there's no way to simply compile the inner class)
        if (dependentClass.name.contains("$")) {
            dependentClassSourcePath = FileUtil.fromParts(sourceDir, dependentClass.name.substring(0, dependentClass.name.indexOf('$'))
                    .replace('.', File.separatorChar) + ".java");
        } else {
            dependentClassSourcePath = FileUtil.fromParts(sourceDir, dependentClass.name.replace('.', File.separatorChar) + ".java");
        }
        return dependentClassSourcePath.getAbsolutePath();
    }

    private void addExistingErrors(String buildDir, Scope scope, PropFile into) {
        File existingErrors = FileUtil.fromParts(buildDir, "compiler-errors" + scope.getFileSuffix() + ".properties");
        if (!existingErrors.exists()) {
            return;
        }
        PropFile errors = PropFiles.load(existingErrors.getPath(), false, false);
        for (PropFile.Prop error : errors.props()) {
            into.add(error.name, "");
        }
    }

}
