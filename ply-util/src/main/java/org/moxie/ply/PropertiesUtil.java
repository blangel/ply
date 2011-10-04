package org.moxie.ply;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

/**
 * User: blangel
 * Date: 10/3/11
 * Time: 6:16 PM
 *
 * Assists in loading and storing {@link java.util.Properties} files.
 */
public class PropertiesUtil {

    /**
     * Loads {@code path} into a {@link Properties} file and returns it.
     * @param path to load
     * @return the loaded {@link Properties} file (which may be empty if loading failed).
     */
    public static Properties load(String path) {
        return load(path, false);
    }

    /**
     * Loads {@code path} into a {@link Properties} file and returns it.
     * @param path to load
     * @param create true will create the file if it does not exist; false otherwise
     * @return the loaded {@link Properties} file (which may be empty if loading failed).
     */
    public static Properties load(String path, boolean create) {
        return load(path, create, false);
    }

    /**
     * Loads {@code path} into a {@link Properties} file and returns it.
     * @param path to load
     * @param create true will create the file if it does not exist; false otherwise
     * @param nullOnFNF true to have null returned when {@code path} can not be found; otherwise an error is printed.
     * @return the loaded {@link Properties} file (which may be empty if loading failed).
     */
    public static Properties load(String path, boolean create, boolean nullOnFNF) {
        File propertiesFile = new File(path);
        InputStream inputStream = null;
        Properties properties = new Properties();
        try {
            if (create && !propertiesFile.exists()) {
                propertiesFile.getParentFile().mkdirs();
                propertiesFile.createNewFile();
            }
            inputStream = new BufferedInputStream(new FileInputStream(propertiesFile));
            properties.load(inputStream);
        } catch (FileNotFoundException fnfe) {
            if (nullOnFNF) {
                return null;
            } else {
                Output.print("Cannot load properties file, %s, it does not exist.", path);
                Output.print(fnfe);
            }
        } catch (IOException ioe) {
            Output.print(ioe);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ioe) {
                    // ignore
                }
            }
        }
        return properties;
    }

    /**
     * Calls {@link #store(java.util.Properties, String, String, boolean)} with null as the comment and false for create
     * @param properties @see {@link #store(java.util.Properties, String, String, boolean)}
     * @param to @see {@link #store(java.util.Properties, String, String, boolean)}
     * @return true on success; false otherwise
     */
    public static boolean store(Properties properties, String to) {
        return store(properties, to, null, false);
    }

    /**
     * Calls {@link #store(java.util.Properties, String, String, boolean)} with null as the comment.
     * @param properties @see {@link #store(java.util.Properties, String, String, boolean)}
     * @param to @see {@link #store(java.util.Properties, String, String, boolean)}
     * @param create @see {@link #store(java.util.Properties, String, String, boolean)}
     * @return true on success; false otherwise
     */
    public static boolean store(Properties properties, String to, boolean create) {
        return store(properties, to, null, create);
    }

    /**
     * Stores {@code properties} into {@code to} with the given {@code comment}.  If {@code create} is true
     * then {@code to} will be created if it does not exist (including necessary directories).
     * @param properties to store
     * @param to which to store {@code properties}
     * @param comment of the property file
     * @param create the {@code to} file if it doesn't exist (including it's sub-directories).
     * @return true on success; false otherwise
     */
    public static boolean store(Properties properties, String to, String comment, boolean create) {
        File propertiesFile = new File(to);
        OutputStream outputStream = null;
        try {
            if (create && !propertiesFile.exists()) {
                propertiesFile.getParentFile().mkdirs();
                propertiesFile.createNewFile();
            }
            outputStream = new BufferedOutputStream(new FileOutputStream(propertiesFile));
            properties.store(outputStream, comment);
            return true;
        } catch (FileNotFoundException fnfe) {
            Output.print("Cannot load properties file, %s, it does not exist.", to);
            Output.print(fnfe);
        } catch (IOException ioe) {
            Output.print(ioe);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException ioe) {
                    // ignore
                }
            }
        }
        return false;
    }
}