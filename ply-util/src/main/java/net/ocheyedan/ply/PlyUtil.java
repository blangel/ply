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
        return "true".equalsIgnoreCase(Props.getValue(Context.named("ply"), "headless"));
    }

    /**
     * @return true if unicode is supported as output
     */
    public static boolean isUnicodeSupported() {
        return "true".equalsIgnoreCase(Props.getValue(Context.named("ply"), "unicode"));
    }

    /**
     * This directory has to be resolved as ply can be invoked from within a nested directory.
     * @return the resolved local ply project directory
     */
    private static File resolveLocalDir() {
        String root = "/.ply";
        String defaultPath = "./.ply";
        String path = defaultPath;
        File ply = new File(path);
        try {
            while (!ply.exists() && !root.equals(ply.getCanonicalPath())) {
                path = "../" + path;
                ply = new File(path);
            }
            if (root.equals(ply.getCanonicalPath())) {
                return new File(defaultPath);
            }
        } catch (IOException ioe) {
            Output.print(ioe);
            return new File(defaultPath);
        }
        return ply;
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
