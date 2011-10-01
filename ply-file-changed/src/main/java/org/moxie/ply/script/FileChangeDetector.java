package org.moxie.ply.script;

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
 * Determines which files within {@literal ply.src.dir} have changed since last invocation.
 * The information used to determine if a file has changed is saved in the {@literal ply.build.dir} in a file named
 * {@literal src-changed-meta.properties}.  The list of files which have changed since last invocation is stored in a file
 * named {@literal src-changed.properties} in directory {@literal ply.build.dir}.
 * The information used to determine change is stored relative to {@literal ply.build.dir} to allow for cleans to
 * force a full-recompilation.  The format of the {@literal src-changed-meta.properties} file is file-path=timestamp,sha1-hash
 * and the format of the {@literal src-changed.properties} is simply a listing of file paths which have changed.
 *
 */
public class FileChangeDetector {

    public static void main(String[] args) {
        String buildDirPath = System.getenv("ply.build.dir");
        File buildDir = new File(buildDirPath);
        File lastSrcChanged = new File(buildDirPath + (buildDirPath.endsWith(File.separator) ? "" : File.separator)
                + "src-changed-meta.properties");
        File changedPropertiesFile = new File(buildDirPath + (buildDirPath.endsWith(File.separator) ? "" : File.separator)
                + "src-changed.properties");
        String srcDirPath = System.getenv("ply.src.dir");
        File srcDir = new File(srcDirPath);
        Properties existing = new Properties();
        if (!lastSrcChanged.exists()) {
            buildDir.mkdirs();
            try {
                lastSrcChanged.createNewFile();
            } catch (IOException ioe) {
                System.out.println("^error^ " + ioe.getMessage());
            }
        } else {
            InputStream fileInputStream = null;
            try {
                fileInputStream = new BufferedInputStream(new FileInputStream(lastSrcChanged));
                existing.load(fileInputStream);
            } catch (IOException ioe) {
                System.out.println("^error^ " + ioe.getMessage());
            } finally {
                try {
                    if (fileInputStream != null) {
                        fileInputStream.close();
                    }
                } catch (IOException ioe) {
                    // ignore
                }
            }
        }
        try {
            changedPropertiesFile.createNewFile();
        } catch (IOException ioe) {
            System.out.println("^error^ " + ioe.getMessage());
        }
        computeFilesChanged(lastSrcChanged, changedPropertiesFile, srcDir, existing);
    }

    private static void computeFilesChanged(File lastSrcChanged, File changedPropertiesFile, File srcDir, Properties existing) {
        Properties changedList = new Properties();
        Properties properties = new Properties();
        OutputStream changedListFileOutputStream = null;
        OutputStream propertiesFileOutputStream = null;
        collectAllFileChanges(srcDir, changedList, properties, existing);
        try {
            changedListFileOutputStream = new BufferedOutputStream(new FileOutputStream(changedPropertiesFile));
            changedList.store(changedListFileOutputStream, null);
            propertiesFileOutputStream = new BufferedOutputStream(new FileOutputStream(lastSrcChanged));
            properties.store(propertiesFileOutputStream, null);
        } catch (IOException ioe) {
            System.out.println("^error^ " + ioe.getMessage());
        } finally {
            try {
                if (changedListFileOutputStream != null) {
                    changedListFileOutputStream.close();
                }
            } catch (IOException ioe) {
                // ignore
            }
            try {
                if (propertiesFileOutputStream != null) {
                    propertiesFileOutputStream.close();
                }
            } catch (IOException ioe) {
                // ignore
            }
        }
    }

    private static void collectAllFileChanges(File from, Properties changedList, Properties into, Properties existing) {
        String epochTime = String.valueOf(System.currentTimeMillis());
        for (File file : from.listFiles()) {
            if (file.isDirectory()) {
                collectAllFileChanges(file, changedList, into, existing);
            } else {
                try {
                    AtomicReference<String> sha1HashRef = new AtomicReference<String>();
                    String path = file.getCanonicalPath();
                    if (hasChanged(file, existing, sha1HashRef)) {
                        String sha1Hash = (sha1HashRef.get() == null ? computeSha1Hash(file) : sha1HashRef.get());
                        into.setProperty(path, epochTime + "," + sha1Hash);
                        changedList.setProperty(path, "");
                    } else {
                        into.setProperty(path, existing.getProperty(path));
                    }
                } catch (IOException ioe) {
                    System.out.println("^error^ " + ioe.getMessage());
                }
            }
        }
    }

    private static boolean hasChanged(File file, Properties existing, AtomicReference<String> computedSha1) {
        try {
            String propertyValue;
            if ((propertyValue = existing.getProperty(file.getCanonicalPath())) == null) {
                return true;
            }
            String[] split = propertyValue.split("\\,");
            if (split.length != 2) {
                System.out.println("^warn^ corrupted src-changed-meta.properties file, recomputing.");
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
            System.out.println("^warn^ corrupted src-changed-meta.properties file, recomputing.");
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
            return toHexString(sha1);
        } catch (NoSuchAlgorithmException nsae) {
            throw new AssertionError(nsae);
        } catch (FileNotFoundException fnfe) {
            throw new AssertionError(fnfe);
        } catch (IOException ioe) {
            System.out.println("^error^ " + ioe.getMessage());
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

    private static String toHexString(byte[] array) {
        if (array == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(array.length * 2);
        for (byte byt : array) {
            int v = byt & 0xff;
            if (v < 16) {
                sb.append('0');
            }
            sb.append(Integer.toHexString(v));
        }
        return sb.toString();
    }
}
