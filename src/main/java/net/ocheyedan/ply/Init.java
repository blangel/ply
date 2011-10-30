package net.ocheyedan.ply;

import java.io.*;
import java.util.Properties;

/**
 * User: blangel
 * Date: 9/5/11
 * Time: 1:55 PM
 *
 * Sets up a local build point within the current working directory.
 *
 */
public class Init {

    public static void invoke(String[] args) {
         // check for existing init.
        File ply = new File("./.ply");
        if (ply.exists()) {
            Output.print("^info^ current directory is already initialized.");
            return;
        }
        ply.mkdir();
        // now create the config options
        File configDir = new File("./.ply/config");
        configDir.mkdirs();
        OutputStream outputStream = null;
        try {
            File propFile = new File("./.ply/config/project.properties");
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
            localProperties.put("namespace", path);
            localProperties.put("name", path);
            localProperties.put("version", "1.0");
            outputStream = new BufferedOutputStream(new FileOutputStream(propFile));
            localProperties.store(outputStream, null);
        } catch (IOException ioe) {
            Output.print("^error^ could not create the local project.properties file.");
            Output.print(ioe);
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException ioe) {
                // ignore
            }
        }
    }

}
