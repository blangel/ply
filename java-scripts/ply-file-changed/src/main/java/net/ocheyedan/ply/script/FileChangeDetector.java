package net.ocheyedan.ply.script;

import net.ocheyedan.ply.BitUtil;
import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.PropertiesFileUtil;
import net.ocheyedan.ply.props.Props;
import net.ocheyedan.ply.props.Scope;

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
 * This scope property (ply.scope) is used as a suffix to the file
 * names created by this script.  If the scope is the default then there will be no suffix.  This file name suffix is
 * referred to as '${suffix}' below.
 *
 * Determines which files within {@literal project[.scope].src.dir} have changed since last invocation.
 * The information used to determine if a file has changed is saved in the {@literal project.build.dir} in a file named
 * {@literal changed-meta[.${suffix}].properties}.  The list of files which have changed since last invocation is stored
 * in a file named {@literal changed[.${suffix}].properties} in directory {@literal project[.scope].build.dir}.
 * The information used to determine change is stored relative to {@literal project[.scope].build.dir} to allow for cleans to
 * force a full-recompilation.  The format of the {@literal changed-meta[.${suffix}].properties} file is:
 * file-path=timestamp,sha1-hash
 * and the format of the {@literal changed[.${suffix}].properties} is simply a listing of file paths which have changed.
 *
 */
public class FileChangeDetector {

    public static void main(String[] args) {
        Scope scope = new Scope(Props.getValue("ply", "scope"));
        String srcDirPath = Props.getValue("project", "src.dir");
        String buildDirPath = Props.getValue("project", "build.dir");
        File lastSrcChanged = FileUtil.fromParts(buildDirPath, "changed-meta" + scope.fileSuffix + ".properties");
        File changedPropertiesFile = FileUtil.fromParts(buildDirPath, "changed" + scope.fileSuffix + ".properties");
        File srcDir = new File(srcDirPath);
        Properties existing = PropertiesFileUtil.load(lastSrcChanged.getPath(), true);
        try {
            changedPropertiesFile.createNewFile();
        } catch (IOException ioe) {
            Output.print(ioe);
        }
        computeFilesChanged(lastSrcChanged, changedPropertiesFile, srcDir, existing, scope);
    }

    private static void computeFilesChanged(File lastSrcChanged, File changedPropertiesFile, File srcDir,
                                            Properties existing, Scope scope) {
        Properties changedList = new Properties();
        Properties properties = new Properties();
        collectAllFileChanges(srcDir, changedList, properties, existing, scope);
        PropertiesFileUtil.store(changedList, changedPropertiesFile.getPath());
        PropertiesFileUtil.store(properties, lastSrcChanged.getPath());
    }

    private static void collectAllFileChanges(File from, Properties changedList, Properties into, Properties existing,
                                              Scope scope) {
        String epochTime = String.valueOf(System.currentTimeMillis());
        File[] subfiles = from.listFiles();
        if (subfiles == null) {
            return;
        }
        for (File file : subfiles) {
            if (file.isDirectory()) {
                collectAllFileChanges(file, changedList, into, existing, scope);
            } else {
                try {
                    AtomicReference<String> sha1HashRef = new AtomicReference<String>();
                    String path = file.getCanonicalPath();
                    if (hasChanged(file, existing, sha1HashRef, scope) && file.exists()) {
                        String sha1Hash = (sha1HashRef.get() == null ? computeSha1Hash(file) : sha1HashRef.get());
                        into.setProperty(path, epochTime + "," + sha1Hash);
                        changedList.setProperty(path, "");
                    } else if (file.exists()) {
                        into.setProperty(path, existing.getProperty(path));
                    }
                } catch (IOException ioe) {
                    Output.print(ioe);
                }
            }
        }
    }

    private static boolean hasChanged(File file, Properties existing, AtomicReference<String> computedSha1, Scope scope) {
        try {
            String propertyValue;
            if ((propertyValue = existing.getProperty(file.getCanonicalPath())) == null) {
                return true;
            }
            String[] split = propertyValue.split("\\,");
            if (split.length != 2) {
                Output.print("^warn^ corrupted changed-meta%s.properties file, recomputing.", scope.fileSuffix);
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
            Output.print("^warn^ corrupted changed-meta%s.properties file, recomputing.", scope.fileSuffix);
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
                throw new AssertionError(ioe);
            }
        }
        return ""; // error!
    }

}