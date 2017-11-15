package net.ocheyedan.ply;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

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
            Output.print("^error^ Could not copy stream to %s", to.getAbsolutePath());
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
     * If {@code url} is local, returns {@link java.net.URL#getFile()} otherwise downloads the url
     * to a temporary file and returns that file's path.
     * @param url to resolve to a local path
     * @param headers to be used when downloading {@code url}
     * @param name of the file to be downloaded, for debug logging
     * @param intoName of the file into which to download, for debug logging
     * @return a local filesystem path to the dependencies file or null on exception
     */
    public static String getLocalPath(URL url, Map<String, String> headers, String name, String intoName) {
        if (url == null) {
            return null;
        }
        String protocol = url.getProtocol();
        if ("file".equals(protocol)) {
            return url.getFile();
        } else {
            try {
                File tmp = File.createTempFile("ply-", ".tmp");
                if (download(url, headers, tmp, name, intoName, true)) {
                    return tmp.getPath();
                } else {
                    return null;
                }
            } catch (IOException ioe) {
                return null; //
            }
        }
    }

    /**
     * Downloads the {@code remoteUrl} and saves to {@code into} file.
     * @param remoteUrl to download
     * @param headers to use when making a connection to {@code remoteUrl}
     * @param into the location into which to download
     * @param name of the file being downloaded
     * @param intoName of the location into which the file is being downloaded
     * @param ignoreFNF true to ignore printing exception messages when file is not found
     * @return true if the file was successfully downloaded and saved {@code into}, false otherwise
     */
    public static boolean download(URL remoteUrl, Map<String, String> headers, File into, String name, String intoName, boolean ignoreFNF) {
        if (remoteUrl == null) {
            return false;
        }
        InputStream stream;
        try {
            // TODO - proxy info (see http://download.oracle.com/javase/6/docs/technotes/guides/net/proxies.html)
            URLConnection urlConnection = remoteUrl.openConnection();
            if (headers != null) {
                for (String key : headers.keySet()) {
                    urlConnection.addRequestProperty(key, headers.get(key));
                }
            }
            stream = urlConnection.getInputStream();
        } catch (FileNotFoundException fnfe) {
            if (!ignoreFNF) {
                Output.print(fnfe);
            }
            return false;
        } catch (UnknownHostException uhe) {
            Output.print("^error^ Could not download %s; remote URL %s not accessible", name, intoName, remoteUrl.getHost());
            Output.print(uhe);
            Output.print("");
            return false;
        } catch (IOException ioe) {
            Output.print(ioe); // TODO - parse exception and more gracefully handle http-errors.
            return false;
        }
        Output.print("^info^ Downloading %s from %s...", name, intoName);
        return FileUtil.copy(stream, into);
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
        String resolved = pathFromParts(USER_HOME, path.substring(1));
        if (PlyUtil.isWindowsOs()) {
            resolved = String.format("file://%s%s", (resolved.startsWith("/") ? "" : "/"), resolved);
        }
        return resolved;
    }

    /**
     * Removes resolved prefix from {@code path} replacing it with a '~'
     * @param path to reverse resolve unix tilde
     * @return {@code path} with '~' replacing resolution or {@code path} as inputted if the value did not start with the
     *         resolved user home directory.
     */
    public static String reverseUnixTilde(String path) {
        if (path.startsWith(USER_HOME)) {
            return path.replace(USER_HOME, "~");
        }
        String prefix = path.startsWith("/") ? "file://" : "file:///";
        if (path.startsWith(String.format("%s%s", prefix, USER_HOME))) {
            return path.replace(String.format("%s%s", prefix, USER_HOME), "~");
        }
        return path;
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

    public static String getSha1Hash(File file) {
        InputStream fileInputStream = null;
        try {
            MessageDigest hash = MessageDigest.getInstance("SHA1");
            fileInputStream = new BufferedInputStream(new FileInputStream(file));
            DigestInputStream digestInputStream = new DigestInputStream(fileInputStream, hash);
            byte[] buffer = new byte[8192];
            while (digestInputStream.read(buffer, 0, buffer.length) != -1) { }
            byte[] sha1 = hash.digest();
            return BitUtil.toHexString(sha1);
        } catch (NoSuchAlgorithmException nsae) {
            throw new AssertionError(nsae);
        } catch (FileNotFoundException fnfe) {
            throw new AssertionError(fnfe);
        } catch (IOException ioe) {
            Output.print(ioe);
        } finally {
            try {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            } catch (IOException ioe) {
                throw new AssertionError(ioe);
            }
        }
        throw new AssertionError();
    }

    private FileUtil() { }

}
