package net.ocheyedan.ply;

import net.ocheyedan.ply.props.Context;
import net.ocheyedan.ply.props.PropFile;
import net.ocheyedan.ply.props.PropFiles;
import net.ocheyedan.ply.props.Props;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

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

    private static final String OS = System.getProperty("os.name");

    private static final String OS_LOWER_CASE = (OS == null ? "" : OS.toLowerCase());

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
     * @param name of the invocation properties file to add
     * @param keys the values to store
     */
    public static void addInvocationProperties(String name, String[] keys, String ... values) {
        if ((keys == null) || (values == null) || (keys.length < 1) || (keys.length != values.length)) {
            throw new AssertionError("Must specify keys and the length must match values' length");
        }
        String path = Props.get("ply.invocation.dir", Context.named("project")).value();
        if ((path == null) || path.isEmpty()) {
            Output.print("^warn^ Property ^b^ply.invocation.dir^r^ not correctly setup in context ^b^project^r^");
            return;
        }
        PropFile propFile = new PropFile(Context.named(""), PropFile.Loc.AdHoc);
        for (int i = 0; i < keys.length; i++) {
            propFile.add(keys[i], values[i]);
        }
        PropFiles.store(propFile, FileUtil.fromParts(path, name).getAbsolutePath(), true);
    }

    /**
     * @param name of the invocation property file
     * @param key of the property to retrieve
     * @return the value associated with {@code key} within the invocation property file {@code name}
     */
    public static String readInvocationProperty(String name, String key) {
        PropFile existing = PlyUtil.readInvocationProperties(name);
        return (existing != null ? existing.get(key).value() : null);
    }

    /**
     * @param name of the invocation properties file
     * @param key to lookup
     * @param expected non-null expected value
     * @return true if there is an invocation property value in file {@code name} with {@code key} which is
     *         equal to {@code expected}
     */
    public static boolean matchingInvocationProperty(String name, String key, String expected) {
        String value = readInvocationProperty(name, key);
        return expected.equals(value);
    }

    /**
     * @param name of the invocation properties file to read
     * @return the properties file or null if none exists
     */
    public static PropFile readInvocationProperties(String name) {
        String path = Props.get("ply.invocation.dir", Context.named("project")).value();
        if ((path == null) || path.isEmpty()) {
            return null;
        }
        File file = FileUtil.fromParts(path, name);
        if (!file.exists()) {
            return null;
        }
        PropFile propFile = new PropFile(Context.named(""), PropFile.Loc.AdHoc);
        PropFiles.load(file.getAbsolutePath(), propFile, false, false);
        return propFile;
    }

    /**
     * Removes the project.ply.invocation.dir directory
     */
    public static void cleanupInvocationProperties() {
        String path = Props.get("ply.invocation.dir", Context.named("project")).value();
        if ((path == null) || path.isEmpty()) {
            return;
        }
        File directory = new File(path);
        if (directory.exists()) {
            FileUtil.delete(directory);
        }
    }

    /**
     * @return true if {@linkplain System#getProperty(String)} for {@literal os.name} contains {@literal windows}
     */
    public static boolean isWindowsOs() {
        return OS_LOWER_CASE.contains("windows");
    }

    public static String[] varargs(String ... args) {
        return args;
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

    /**
     * @return the IP address of the machine
     */
    public static String getIPAddress() {
        InetAddress inetAddress = null;
        try {
            inetAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException ex) {
            // ignore
        }
        return (inetAddress == null ? "<unknown>" : inetAddress.getHostAddress());
    }

    private PlyUtil() { }

}
