package net.ocheyedan.ply.script;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.dep.DependencyAtom;
import net.ocheyedan.ply.dep.Deps;
import net.ocheyedan.ply.jna.JnaAccessor;
import net.ocheyedan.ply.props.Context;
import net.ocheyedan.ply.props.Props;

import java.io.File;
import java.util.Properties;

/**
 * User: blangel
 * Date: 11/5/11
 * Time: 1:54 PM
 *
 * Extends {@link JarPackageScript} to store as a '.war' file with web.xml and lib information.  The properties are specified
 * in the same {@literal package[.scope].properties} file and this script takes into account these additional properties:
 * webapp.dir=string [[default=src/main/webapp]] (the parent directory of the WEB-INF directory).
 * exploded.war.dir=string [[default=${project.build.dir}/${project.artifact.name} (sans the package)]] (the location
 *                  in which to store the exploded war)
 */
public class WarPackageScript extends JarPackageScript implements PackagingScript {

    @Override protected String getType() {
        return "war";
    }

    /**
     * Includes the exploded directory view of the war (created via {@link #preprocess()}
     * @param compileDir the directory containing compiled output.
     * @param resourceDir the directory where resources are.
     * @return { "-C", "", "." }
     */
    @Override protected String[] getIncludes(String compileDir, String resourceDir) {
        String manifestFile = getManifestFilePath();
        String explodedWarPath = getExplodedWarDirPath();
        return new String[] { manifestFile, "-C", explodedWarPath, "." };
    }

    /**
     * Creates an exploded view of the war.
     */
    @Override protected void preprocess() {
        super.preprocess();
        String buildDir = getBuildDir();
        // create the exploded war directory
        File explodedWarDir = new File(getExplodedWarDirPath());
        explodedWarDir.mkdirs();
        // get the web-inf dir and copy to the exploded war web-inf dir
        String webappDir = Props.getValue(Context.named("package"), "webapp.dir");
        FileUtil.copyDir(FileUtil.fromParts(webappDir, "WEB-INF"),
                FileUtil.fromParts(explodedWarDir.getPath(), "WEB-INF"));
        // copy the meta-dir to the exploded war meta-inf dir
        FileUtil.copyDir(FileUtil.fromParts(buildDir, "META-INF"),
                FileUtil.fromParts(explodedWarDir.getPath(), "META-INF"));
        // copy the classes/resources directory to the WEB-INF/classes
        String buildPath = Props.getValue(Context.named("compiler"), "build.path");
        String resBuildPath = Props.getValue(Context.named("project"), "res.build.dir");
        File buildPathDir = new File(buildPath);
        File resBuildPathDir = new File(resBuildPath);
        if (buildPathDir.exists()) {
            FileUtil.copyDir(buildPathDir, FileUtil.fromParts(explodedWarDir.getPath(), "WEB-INF", "classes"));
        }
        if (resBuildPathDir.exists()) {
            FileUtil.copyDir(resBuildPathDir, FileUtil.fromParts(explodedWarDir.getPath(), "WEB-INF", "classes"));
        }
        // copy the dependencies to the WEB-INF/lib directory
        Properties resolvedProperties = Deps.getResolvedProperties(false);
        Output.print("^info^ Copying dependencies for war file.");
        copyDependencies(resolvedProperties, FileUtil.fromParts(explodedWarDir.getPath(), "WEB-INF", "lib"));
    }

    protected String getExplodedWarDirPath() {
        String explodedWarDir = Props.getValue(Context.named("package"), "exploded.war.dir");
        if (explodedWarDir.endsWith(".war")) {
            explodedWarDir = explodedWarDir.substring(0, explodedWarDir.length() - 4);
        }
        return explodedWarDir;
    }

    protected void copyDependencies(Properties resolvedProperties, File copyToDir) {
        copyToDir.mkdirs();
        for (String resolvedKey : resolvedProperties.stringPropertyNames()) {
            if (DependencyAtom.isTransient(resolvedKey)) {
                continue;
            }
            File dependency = new File(resolvedProperties.getProperty(resolvedKey));
            File to = FileUtil.fromParts(copyToDir.getPath(), dependency.getName());
            if (!to.exists()) {
                if (JnaAccessor.getCUnixLibrary() != null) {
                    JnaAccessor.getCUnixLibrary().symlink(dependency.getPath(), to.getPath());
                } else {
                    FileUtil.copy(dependency, to);
                }
            }
        }
    }

    @Override protected int postprocess(int exitCode) {
        return exitCode; // do nothing, war files already include-deps which is all super does if necessary
    }
}
