package org.moxie.ply.script;

import org.moxie.ply.BitUtil;
import org.moxie.ply.Output;
import org.moxie.ply.PropertiesUtil;

import java.io.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * User: blangel
 * Date: 9/12/11
 * Time: 9:20 PM
 *
 * There are two arguments to this script, both are optional.  The first is a name which is used in the outputted file
 * names.  The default is {@literal src}.  It will be referred to as {@literal args[0]} below.  The second is the
 * directory to process.  The default is the value of {@literal project.src.dir}.  It will be referred to as
 * {@literal args[1]} below.
 *
 * Determines which files within {@literal args[1]} have changed since last invocation.
 * The information used to determine if a file has changed is saved in the {@literal project.build.dir} in a file named
 * {@literal ${args[0]}-changed-meta.properties}.  The list of files which have changed since last invocation is stored
 * in a file named {@literal ${args[0]}-changed.properties} in directory {@literal project.build.dir}.
 * The information used to determine change is stored relative to {@literal project.build.dir} to allow for cleans to
 * force a full-recompilation.  The format of the {@literal ${args[0]}-changed-meta.properties} file is:
 * file-path=timestamp,sha1-hash
 * and the format of the {@literal ${args[0]}-changed.properties} is simply a listing of file paths which have changed.
 *
 */
public class FileChangeDetector {

    public static void main(String[] args) {
        String invocationName = (args.length > 0 ? args[0] : "src");
        String srcDirPath = (args.length > 1 ? args[1] : System.getenv("project.src.dir"));
        Output.print("^dbug^ Invocation name ^b^%s^r^ and source path ^b^%s^r^.", invocationName, srcDirPath);
        String buildDirPath = System.getenv("project.build.dir");
        File lastSrcChanged = new File(buildDirPath + (buildDirPath.endsWith(File.separator) ? "" : File.separator)
                + invocationName + "-changed-meta.properties");
        File changedPropertiesFile = new File(buildDirPath + (buildDirPath.endsWith(File.separator) ? "" : File.separator)
                + invocationName + "-changed.properties");
        File srcDir = new File(srcDirPath);
        Properties existing = PropertiesUtil.load(lastSrcChanged.getPath(), true);
        try {
            changedPropertiesFile.createNewFile();
        } catch (IOException ioe) {
            Output.print(ioe);
        }
        computeFilesChanged(lastSrcChanged, changedPropertiesFile, srcDir, existing, invocationName);
    }

    private static void computeFilesChanged(File lastSrcChanged, File changedPropertiesFile, File srcDir,
                                            Properties existing, String invocationName) {
        Properties changedList = new Properties();
        Properties properties = new Properties();
        collectAllFileChanges(srcDir, changedList, properties, existing, invocationName);
        PropertiesUtil.store(changedList, changedPropertiesFile.getPath());
        PropertiesUtil.store(properties, lastSrcChanged.getPath());
    }

    private static void collectAllFileChanges(File from, Properties changedList, Properties into, Properties existing,
                                              String invocationName) {
        String epochTime = String.valueOf(System.currentTimeMillis());
        for (File file : from.listFiles()) {
            if (file.isDirectory()) {
                collectAllFileChanges(file, changedList, into, existing, invocationName);
            } else {
                try {
                    AtomicReference<String> sha1HashRef = new AtomicReference<String>();
                    String path = file.getCanonicalPath();
                    if (hasChanged(file, existing, sha1HashRef, invocationName)) {
                        String sha1Hash = (sha1HashRef.get() == null ? computeSha1Hash(file) : sha1HashRef.get());
                        into.setProperty(path, epochTime + "," + sha1Hash);
                        changedList.setProperty(path, "");
                    } else {
                        into.setProperty(path, existing.getProperty(path));
                    }
                } catch (IOException ioe) {
                    Output.print(ioe);
                }
            }
        }
    }

    private static boolean hasChanged(File file, Properties existing, AtomicReference<String> computedSha1, String invocationName) {
        try {
            String propertyValue;
            if ((propertyValue = existing.getProperty(file.getCanonicalPath())) == null) {
                return true;
            }
            String[] split = propertyValue.split("\\,");
            if (split.length != 2) {
                Output.print("^warn^ corrupted %s-changed-meta.properties file, recomputing.", invocationName);
                return true;
            }
            long timestamp = Long.valueOf(split[0]);
            if (file.lastModified() == timestamp) {
                return false;
            }
            String oldHashAsHex = split[1];
            String asHex = computeSha1Hash(file);
            computedSha1.set(asHex);
            return !asHex.equals(oldHashAsHex);
        } catch (IOException ioe) {
            throw new AssertionError(ioe);
        } catch (NumberFormatException nfe) {
            Output.print("^warn^ corrupted %s-changed-meta.properties file, recomputing.", invocationName);
            return true;
        }
    }

    private static String computeSha1Hash(File file) {
        InputStream fileInputStream = null;
        try {
            MessageDigest hash = MessageDigest.getInstance("SHA1");
            fileInputStream = new BufferedInputStream(new FileInputStream(file));
            DigestInputStream digestInputStream = new DigestInputStream(fileInputStream, hash);
            byte[] buffer = new byte[8192];
            while (digestInputStream.read(buffer, 0, 8192) != -1) { }
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
                // ignore
            }
        }
        return ""; // error!
    }

}