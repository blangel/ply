package org.moxie.ply;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * User: blangel
 * Date: 9/2/11
 * Time: 4:24 PM
 *
 * Sets up a local build point within the current working directory.
 */
public class Init {

    public static void invoke(String[] args) {
        // check for existing init.
        File ply = new File("./.ply");
        if (ply.exists()) {
            Output.print("^info^ current directory is already initialized.^r^");
            return;
        }
        ply.mkdir();
        // now create the config options
        Config.LOCAL_CONFIG_DIR.mkdirs();
        try {
            Config.LOCAL_PROPS_FILE.createNewFile();
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
            localProperties.store(new FileOutputStream(Config.LOCAL_PROPS_FILE), null);
        } catch (IOException ioe) {
            Output.print("^error^ could not create the local ply.properties file.");
            Output.print(ioe);
        }
    }
    
}
