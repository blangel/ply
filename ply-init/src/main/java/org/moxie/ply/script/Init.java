package org.moxie.ply.script;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * User: blangel
 * Date: 9/5/11
 * Time: 1:55 PM
 *
 * Sets up a local build point within the current working directory.
 */
public class Init {

    public static void main(String[] args) {
         // check for existing init.
        File ply = new File("./.ply");
        if (ply.exists()) {
            System.out.print("^info^ current directory is already initialized.");
            return;
        }
        ply.mkdir();
        // now create the config options
        File configDir = new File("./.ply/config");
        configDir.mkdirs();
        try {
            File propFile = new File("./.ply/config/ply.properties");
            propFile.createNewFile();
            Properties localProperties = new Properties();
            File projectDirectory = new File(".");
            String path = projectDirectory.getCanonicalPath();
            if (path.endsWith(File.separator)) {
                path = path.substring(0, path.length() - 1);
            }
            int lastPathIndex = path.lastIndexOf(File.separator);
            if (lastPathIndex != -1) {
                path = path.substring(lastPathIndex + 1);
            }
            localProperties.put("project.name", path);
            localProperties.store(new FileOutputStream(propFile), null);
        } catch (IOException ioe) {
            System.out.print("^error^ could not create the local ply.properties file.");
            System.out.printf("^error^ Message: ^i^^red^%s^r^", ioe.getMessage());
        }
    }

}
