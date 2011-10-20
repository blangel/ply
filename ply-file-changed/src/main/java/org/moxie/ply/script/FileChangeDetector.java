package org.moxie.ply.script;

import org.moxie.ply.BitUtil;
import org.moxie.ply.Output;
import org.moxie.ply.PropertiesFileUtil;
import org.moxie.ply.props.Props;

import java.io.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

/**
 * User: blangel
 * Date: 9/12/11
 * Time: 9:20 PM
 *
 * This scope property (ply.scope) is used as a prefix to the file
 * names created by this script.  If the scope is null then 'src' will be used for file names prefix.  This file name
 * prefix is referred to as '${prefix}' below.
 *
 * Determines which files within {@literal project[.scope].src.dir} have changed since last invocation.
 * The information used to determine if a file has changed is saved in the {@literal project.build.dir} in a file named
 * {@literal ${prefix}-changed-meta.properties}.  The list of files which have changed since last invocation is stored
 * in a file named {@literal ${prefix}-changed.properties} in directory {@literal project[.scope].build.dir}.
 * The information used to determine change is stored relative to {@literal project[.scope].build.dir} to allow for cleans to
 * force a full-recompilation.  The format of the {@literal ${prefix}-changed-meta.properties} file is:
 * file-path=timestamp,sha1-hash
 * and the format of the {@literal ${prefix}-changed.properties} is simply a listing of file paths which have changed.
 *
 */
public class FileChangeDetector {

    public static void main(String[] args) {
        String scope = Props.getValue("ply", "scope");
        String prefix = (scope.isEmpty() ? "src" : scope);
        String srcDirPath = Props.getValue("project", "src.dir");
        String buildDirPath = Props.getValue("project", "build.dir");
        File lastSrcChanged = new File(buildDirPath + (buildDirPath.endsWith(File.separator) ? "" : File.separator)
                + prefix + "-changed-meta.properties");
        File changedPropertiesFile = new File(buildDirPath + (buildDirPath.endsWith(File.separator) ? "" : File.separator)
                + prefix + "-changed.properties");
        File srcDir = new File(srcDirPath);
        Properties existing = PropertiesFileUtil.load(lastSrcChanged.getPath(), true);
        try {
            changedPropertiesFile.createNewFile();
        } catch (IOException ioe) {
            Output.print(ioe);
        }
        computeFilesChanged(lastSrcChanged, changedPropertiesFile, srcDir, existing, prefix);
    }

    private static void computeFilesChanged(File lastSrcChanged, File changedPropertiesFile, File srcDir,
                                            Properties existing, String invocationName) {
        Properties changedList = new Properties();
        Properties properties = new Properties();
        collectAllFileChanges(srcDir, changedList, properties, existing, invocationName);
        PropertiesFileUtil.store(changedList, changedPropertiesFile.getPath());
        PropertiesFileUtil.store(properties, lastSrcChanged.getPath());
    }

    private static void collectAllFileChanges(File from, Properties changedList, Properties into, Properties existing,
                                              String invocationName) {
        String epochTime = String.valueOf(System.currentTimeMillis());
        File[] subfiles = from.listFiles();
        if (subfiles == null) {
            return;
        }
        for (File file : subfiles) {
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