package net.ocheyedan.ply;

import net.ocheyedan.ply.props.Context;
import net.ocheyedan.ply.props.Props;

import java.io.File;
import java.io.IOException;

/**
 * User: blangel
 * Date: 10/6/11
 * Time: 11:40 AM
 *
 * Utility class to retrieve ply specific information like location of the ply installation and project directories.
 */
public final class PlyUtil {

    /**
     * The directory in which ply is installed, passed in by the invoking script.
     */
    public static final String INSTALL_DIRECTORY = System.getProperty("ply.home");

    /**
     * The current version of the running ply program, passed in by the invoking script.
     */
    public static final String PLY_VERSION = System.getProperty("ply.version");

    /**
     * The system configuration directory (in which property files are stored) for the ply system.
     */
    public static final File SYSTEM_CONFIG_DIR = new File(INSTALL_DIRECTORY + File.separator + "config");

    /**
     * The system scripts directory for the ply system.
     */
    public static final File SYSTEM_SCRIPTS_DIR = new File(INSTALL_DIRECTORY + File.separator + "scripts");

    /**
     * The local project directory (local to the init-ed project).
     */
    public static final File LOCAL_PROJECT_DIR = resolveLocalDir();

    /**
     * The local configuration directory (local to the init-ed project).
     */
    public static final File LOCAL_CONFIG_DIR = resolveLocalConfigDir(LOCAL_PROJECT_DIR);

    /**
     * @return true if all prompts should be disallowed.
     */
    public static boolean isHeadless() {
        return "true".equalsIgnoreCase(Props.get("headless", Context.named("ply")).value());
    }

    /**
     * @return true if unicode is supported as output
     */
    public static boolean isUnicodeSupported() {
        return "true".equalsIgnoreCase(Props.get("unicode", Context.named("ply")).value());
    }

    public static String getProjectDir(File configDirectory) {
        return FileUtil.getCanonicalPath(FileUtil.fromParts(configDirectory.getPath(), ".."));
    }

    /**
     * This directory has to be resolved as ply can be invoked from within a nested directory.
     * @return the resolved local ply project directory
     */
    private static File resolveLocalDir() {
        File[] roots = File.listRoots();
        String defaultPath = "./.ply";
        String path = defaultPath;
        File ply = new File(path);
        boolean atRoot = false;
        try {
            while (!ply.exists() && !(atRoot = reachedRoot(roots, ".ply", ply.getCanonicalPath()))) {
                path = "../" + path;
                ply = new File(path);
            }
            if (atRoot) {
                return new File(defaultPath);
            }
        } catch (IOException ioe) {
            Output.print(ioe);
            return new File(defaultPath);
        }
        return ply;
    }

    /**
     * Returns true if any {@code roots} combined with {@code rootSuffix} equals {@code canonicalPath}
     * @param roots to check
     * @param rootSuffix to add to the end of each {@code roots} when evaluating
     * @param canonicalPath to match
     * @return true if any {@code roots} combined with {@code rootSuffix} equals {@code canonicalPath}
     */
    private static boolean reachedRoot(File[] roots, String rootSuffix, String canonicalPath) {
        for (File root : roots) {
            if (canonicalPath.equals(root.getPath() + rootSuffix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param resolvedLocalProjectDir @see {@link #resolveLocalDir()}
     * @return the local config directory
     */
    private static File resolveLocalConfigDir(File resolvedLocalProjectDir) {
        try {
            String path = resolvedLocalProjectDir.getCanonicalPath();
            return new File(path.endsWith(File.separator) ? path + "config" : path + File.separator + "config");
        } catch (IOException ioe) {
            Output.print(ioe);
            SystemExit.exit(1);
            return null; // not reachable
        }
    }

    private PlyUtil() { }

}
