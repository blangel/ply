package net.ocheyedan.ply.script;

import net.ocheyedan.ply.Output;

import java.io.IOException;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * User: blangel
 * Date: 11/16/11
 * Time: 6:25 PM
 *
 * Utility for interacting with {@link ZipFile} objects.
 */
public final class ZipFiles {

    /**
     * Appends all entries within {@code zip} into {@code intoZip}.
     * Note, the {@code intoZip} is not closed by this method to facilitate multiple append calls before closing
     * the zip.
     * @param zip from which to append
     * @param intoZip to which to append
     * @param existing {@link ZipEntry} names to avoid duplicate entries exceptions.
     * @throws IOException @see {@link ZipOutputStream#write(byte[])}
     */
    public static void append(ZipInputStream zip, ZipOutputStream intoZip, Set<String> existing) throws IOException {
        if ((zip == null) || (intoZip == null)) {
            return;
        }
        ZipEntry entry;
        int len; byte[] buf = new byte[1024];
        while ((entry = zip.getNextEntry()) != null) {
            String entryName = entry.getName();
            if (!existing.add(entryName)) {
                if (!entry.isDirectory() && !"META-INF/MANIFEST.MF".equals(entryName)
                        && !"META-INF/ply/dependencies.properties".equals(entryName)) {
                    Output.print("^warn^ Duplicate entry ^b^%s^r^ skipped.", entryName);
                }
                continue;
            }
            intoZip.putNextEntry(entry);
            while ((len = zip.read(buf)) > 0) {
                intoZip.write(buf, 0, len);
            }
            intoZip.closeEntry();
        }
        zip.close();
    }

    private ZipFiles() { }

}
