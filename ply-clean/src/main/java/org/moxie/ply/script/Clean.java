package org.moxie.ply.script;

import java.io.File;

/**
 * User: blangel
 * Date: 9/9/11
 * Time: 8:57 PM
 *
 * Removes the build.dir directory.
 */
public class Clean {

    public static void main(String[] args) {
        String buildDirPath = System.getenv("build.dir");
        File buildDir = new File(buildDirPath);
        if (buildDir.exists()) {
            delete(buildDir);
        }
    }

    private static void delete(File file) {
        if (file.isDirectory()) {
            for (File subFile : file.listFiles()) {
                delete(subFile);
            }
        }
        if (!file.delete()) {
            System.out.println(String.format("^error^ could not delete file ^b^%s^r^", file.getPath()));
        }
    }

}
