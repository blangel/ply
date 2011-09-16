package org.moxie.ply.script;

import java.io.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * User: blangel
 * Date: 9/12/11
 * Time: 9:20 PM
 *
 * Determines which files within {@literal ply.src.dir} have changed since last invocation.
 * The information used to determine if a file has changed is saved in the {@literal ply.build.dir} in a file named
 * {@literal src-changed.properties}.  The list of files which have changed since last invocation is stored in a file
 * named {@literal changed.properties} in directory {@literal ${project.root.dir}/.ply/filechangedetector/}.
 * The information used to determine change is stored relative to {@literal ply.build.dir} to allow for cleans to
 * force a full-recompilation.  The format of the {@literal src-changed.properties} file is file-path=timestamp,sha1-hash
 * and the format of the {@literal changed.properties} is simply a listing of file paths which have changed.
 *
 */
public class FileChangeDetector {

    public static void main(String[] args) {
        String buildDirPath = System.getenv("ply.build.dir");
        File buildDir = new File(buildDirPath);
        File lastSrcChanged = new File(buildDirPath + (buildDirPath.endsWith(File.separator) ? "" : File.separator)
                + "src-changed.properties");
        String srcDirPath = System.getenv("ply.src.dir");
        File srcDir = new File(srcDirPath);
        if (!lastSrcChanged.exists()) {
            buildDir.mkdirs();
            computeFromClean(lastSrcChanged, srcDir);
        } else {
            // todo
        }
    }

    private static void computeFromClean(File lastSrcChanged, File srcDir) {
        Properties properties = new Properties();
	Properties existing = new Properties();
        FileOutputStream propertiesFileOutputStream = null;
        collectAllFileChanges(srcDir, properties, existing);
        try {
            lastSrcChanged.createNewFile();
            propertiesFileOutputStream = new FileOutputStream(lastSrcChanged);
            properties.store(propertiesFileOutputStream, null);
        } catch (IOException ioe) {
            System.out.println("^error^ " + ioe.getMessage());
        } finally {
            try {
                if (propertiesFileOutputStream != null) {
                    propertiesFileOutputStream.close();
                }
            } catch (IOException ioe) {
                // ignore
            }
        }
    }

    private static void collectAllFileChanges(File from, Properties into, Properties existing) {
        String epochTime = String.valueOf(System.currentTimeMillis());
        for (File file : from.listFiles()) {
            if (file.isDirectory()) {
                collectAllFileChanges(file, into);
            } else {
                FileInputStream fileInputStream = null;
                try {
                    MessageDigest hash = MessageDigest.getInstance("SHA1");
                    fileInputStream = new FileInputStream(file);
                    DigestInputStream digestInputStream = new DigestInputStream(fileInputStream, hash);
                    byte[] buffer = new byte[4096];
                    while (digestInputStream.read(buffer, 0, 4096) != -1) { }
                    byte[] sha1 = hash.digest();
                    into.setProperty(file.getCanonicalPath(), epochTime + "," + toHexString(sha1));
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

            }
        }
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
