package net.ocheyedan.ply;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

/**
 * User: blangel
 * Date: 10/3/11
 * Time: 6:35 PM
 *
 * Provides utilities when interacting with {@link File} objects.
 */
public final class FileUtil {

    /**
     * Copies the contents of {@code fromDir} to {@code toDir} recursively.
     * @param fromDir from which to copy
     * @param toDir to which to copy
     * @return true on success; false otherwise
     */
    public static boolean copyDir(File fromDir, File toDir) {
        return copyDir(fromDir, toDir, null);
    }

    /**
     * Copies the contents of {@code fromDir} to {@code toDir} recursively, excluding any matches from {@code excluding}.
     * @param fromDir from which to copy
     * @param toDir to which to copy
     * @param excluding if true will skip copy.  If the value is null, no files/directories will be excluded.
     * @return true on success; false otherwise
     */
    public static boolean copyDir(File fromDir, File toDir, FilenameFilter excluding) {
        if (!fromDir.isDirectory() || !fromDir.exists()) {
            return false;
        }
        toDir.mkdirs();
        for (File subFile : fromDir.listFiles()) {
            if ((excluding != null) && excluding.accept(subFile.getParentFile(), subFile.getName())) {
                continue;
            }
            File toDirSubFile = new File(toDir.getPath() + File.separator + subFile.getName());
            if (subFile.isDirectory()) {
                if (!copyDir(subFile, toDirSubFile)) {
                    return false;
                }
            } else if (!copy(subFile, toDirSubFile)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Copies {@code from} to {@code to}, creating all directories up to {@code to} and the file itself.
     * @param from which to copy
     * @param to which to copy
     * @return true on success; false otherwise
     */
    public static boolean copy(File from, File to) {
        try {
            return copy(new BufferedInputStream(new FileInputStream(from)), to);
        } catch (FileNotFoundException fnfe) {
            Output.print(fnfe);
        }
        return false;
    }

    /**
     * Saves {@code from} to {@code to}.
     * @param from which to copy
     * @param to which to copy
     * @return true if success; false otherwise
     */
    public static boolean copy(URL from, File to) {
        InputStream inputStream = null;
        try {
            URLConnection urlConnection = from.openConnection(); // TODO - proxy info (see http://download.oracle.com/javase/6/docs/technotes/guides/net/proxies.html)
            // keep this small, this is not a server, if there's an issue the user can retry.  typically, running
            // user programs, the user wants this to fail fast so that they can retry.
            urlConnection.setConnectTimeout(1000);
            inputStream = urlConnection.getInputStream();
            return copy(inputStream, to);
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
        return false;
    }

    /**
     * Copies {@code from} to {@code to}.  Creates {@code to} if it does not exist (including any sub-directory).
     * @param from which to copy
     * @param to which to copy
     * @return true if success; false otherwise
     */
    public static boolean copy(InputStream from, File to) {
        OutputStream outputStream = null;
        try {
            if (!to.exists()) {
                to.getParentFile().mkdirs();
                to.createNewFile();
            }
            outputStream = new BufferedOutputStream(new FileOutputStream(to));
            byte[] tx = new byte[8192];
            int read;
            while ((read = from.read(tx)) != -1) {
                outputStream.write(tx, 0, read);
            }
            return true;
        } catch (IOException ioe) {
            Output.print(ioe);
        } finally {
            try {
                if (from != null) {
                    from.close();
                }
            } catch (IOException ioe) {
                throw new AssertionError(ioe);
            }
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException ioe) {
                throw new AssertionError(ioe);
            }
        }
        return false;
    }

    /**
     * Concatenates {@code parts} together ensuring they are correctly separated by {@link File#separator} where
     * appropriate.
     * @param parts to concatenate.
     * @return the path created from {@code parts} (with the appropriate {@link File#separator} separating them).
     */
    public static String pathFromParts(String ... parts) {
        if ((parts == null) || (parts.length < 1)) {
            return null;
        }
        StringBuilder buf = new StringBuilder();
        for (String part : parts) {
            if ((buf.length() > 0) && (buf.charAt(buf.length() - 1) != File.separatorChar)
                    && !part.startsWith(File.separator)) {
                buf.append(File.separatorChar);
            }
            buf.append(part);
        }
        return buf.toString();
    }

    /**
     * Concatenates {@code parts} together ensuring they are correctly separated by {@link File#separator} where
     * appropriate.
     * @param parts to concatenate.
     * @return a {@link File} created from {@code parts} (with the appropriate {@link File#separator} separating them).
     */
    public static File fromParts(String ... parts) {
        String path = pathFromParts(parts);
        if (path == null) {
            return null;
        } else {
            return new File(path);
        }
    }

    private static final String USER_HOME = System.getProperty("user.home");

    /**
     * If {@code path} starts with the {@literal ~} character, treats it like the Unix-convention of tilde being a
     * placeholder for the user home directory and return the resolved path (that is replace the {@literal ~} character
     * with the value of {@link System#getProperty(String)} with parameter {@literal user.home}.
     * @param path to resolve
     * @return the resolved {@code path} if it starts with a {@literal ~} or simply the input {@code path} if it doesn't.
     */
    public static String resolveUnixTilde(String path) {
        if (!path.startsWith("~") || USER_HOME == null) {
            return path;
        }
        return pathFromParts(USER_HOME, path.substring(1));
    }

    /**
     * Deletes {@code file} and if it is a directory recursively deletes all its files and sub-directories (like
     * {@literal rm -rf} would).
     * @param file to delete
     */
    public static void delete(File file) {
        if (file.isDirectory()) {
            for (File subFile : file.listFiles()) {
                delete(subFile);
            }
        }
        if (!file.delete()) {
            Output.print("^error^ could not delete file ^b^%s^r^", file.getPath());
        }
    }

    /**
     * @param file of which to get the canonical path
     * @return the result of calling {@link java.io.File#getCanonicalPath()}
     */
    public static String getCanonicalPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException ioe) {
            Output.print("^error^ Could not get the canonical path of file %s", file.getPath());
            throw new RuntimeException(ioe);
        }
    }

    /**
     * Strips the {@literal URI} {@literal file:} prefix from {@code path}.  The {@linkplain File} class does not
     * parse/support the prefix.
     * @param path from which to strip the leading {@literal URI} {@literal file:} prefix
     * @return {@code path} stripped of any of the following if they are the start of {@code path}: {@literal file://},
     *         {@literal file:/}, and {@literal file:}
     */
    public static String stripFileUriPrefix(String path) {
        if (path == null) {
            return path;
        }
        if (path.startsWith("file://")) {
            return path.substring(7);
        } else if (path.startsWith("file:/")) {
            return path.substring(6);
        } else if (path.startsWith("file:")) {
            return path.substring(5);
        }
        return path;
    }

    private FileUtil() { }

}
