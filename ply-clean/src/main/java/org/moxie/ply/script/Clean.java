package org.moxie.ply.script;

import java.io.File;

/**
 * User: blangel
 * Date: 9/9/11
 * Time: 8:57 PM
 *
 * Removes the {@literal ply.build.dir} directory.
 */
public class Clean {

    public static void main(String[] args) {
        Clean clean = new Clean(System.getenv("ply$project.build.dir"));
        clean.invoke();
    }

    private final String buildDirPath;

    private Clean(String buildDirPath) {
        this.buildDirPath = buildDirPath;
    }

    private void invoke() {
        File buildDir = new File(buildDirPath);
        if (buildDir.exists()) {
            delete(buildDir);
        }
    }

    private void delete(File file) {
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
