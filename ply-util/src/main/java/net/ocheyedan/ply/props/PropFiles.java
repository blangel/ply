package net.ocheyedan.ply.props;

import net.ocheyedan.ply.Output;

import java.io.*;

/**
 * User: blangel
 * Date: 2/18/12
 * Time: 7:14 AM
 * 
 * Assists in loading and saving of {@link PropFile} objects.
 */
public final class PropFiles {

    /**
     * Calls {@link #load(String, PropFileReader, PropFile, boolean, boolean)} with the {@link PropFileReader#Default}
     * as the {@link PropFileReader} implementation, true for the printOnFNF argument and false for the create argument.
     * @param path @see {@link #load(String, PropFileReader, PropFile, boolean, boolean)}
     * @param into @see {@link #load(String, PropFileReader, PropFile, boolean, boolean)}
     * @return @see {@link #load(String, PropFileReader, PropFile, boolean, boolean)}
     * @throws PropFileReader.Invalid @see {@link #load(String, PropFileReader, PropFile, boolean, boolean)}
     */
    public static boolean load(String path, PropFile into) throws PropFileReader.Invalid {
        return load(path, PropFileReader.Default, into, false, true);
    }

    /**
     * Calls {@link #load(String, PropFileReader, PropFile, boolean, boolean)} with the {@link PropFileReader#Default}
     * as the {@link PropFileReader} implementation and true for the printOnFNF argument.
     * @param path @see {@link #load(String, PropFileReader, PropFile, boolean, boolean)}
     * @param into @see {@link #load(String, PropFileReader, PropFile, boolean, boolean)}
     * @param create @see {@link #load(String, PropFileReader, PropFile, boolean, boolean)}
     * @return @see {@link #load(String, PropFileReader, PropFile, boolean, boolean)}
     * @throws PropFileReader.Invalid @see {@link #load(String, PropFileReader, PropFile, boolean, boolean)}
     */
    public static boolean load(String path, PropFile into, boolean create) throws PropFileReader.Invalid {
        return load(path, PropFileReader.Default, into, create, true);
    }

    /**
     * Calls {@link #load(String, PropFileReader, PropFile, boolean, boolean)} with the {@link PropFileReader#Default}
     * as the {@link PropFileReader} implementation.
     * @param path @see {@link #load(String, PropFileReader, PropFile, boolean, boolean)}
     * @param into @see {@link #load(String, PropFileReader, PropFile, boolean, boolean)}
     * @param create @see {@link #load(String, PropFileReader, PropFile, boolean, boolean)}
     * @param printOnFNF @see {@link #load(String, PropFileReader, PropFile, boolean, boolean)}
     * @return @see {@link #load(String, PropFileReader, PropFile, boolean, boolean)}
     * @throws PropFileReader.Invalid @see {@link #load(String, PropFileReader, PropFile, boolean, boolean)}
     */
    public static boolean load(String path, PropFile into, boolean create, boolean printOnFNF) 
            throws PropFileReader.Invalid {
        return load(path, PropFileReader.Default, into, create, printOnFNF);
    }

    /**
     * Calls {@link #load(String, PropFileReader, PropFile, boolean, boolean)} with the {@link PropFileReader#Default}
     * as the {@link PropFileReader} implementation.  It creates a {@link PropFile} based upon the {@code path}.
     * @param path  @see {@link #load(String, PropFileReader, PropFile, boolean, boolean)}
     * @param create  @see {@link #load(String, PropFileReader, PropFile, boolean, boolean)}
     * @param nullOnFNF if {@link #load(String, PropFileReader, PropFile, boolean, boolean)} returns false then this 
     *                  method will return null.
     * @return the loaded {@link PropFile} or the empty file {@link PropFile#Empty} if the file could not be loaded
     *         and {@code nullOnFNF} was false, otherwise null.
     * @throws PropFileReader.Invalid @see {@link #load(String, PropFileReader, PropFile, boolean, boolean)}
     */
    public static PropFile load(String path, boolean create, boolean nullOnFNF)
            throws PropFileReader.Invalid {
        if (path == null) {
            throw new NullPointerException("The path to load must not be null.");
        }
        String name = new File(path).getPath();
        if (name.endsWith(".properties")) {
            name = name.substring(0, name.length() - ".properties".length());
        }
        Context context; 
        Scope scope = Scope.Default;
        if (name.contains(".")) {
            int index = name.indexOf(".");
            context = Context.named(name.substring(0, index));
            scope = Scope.named(name.substring(index + 1));
        } else {
            context = Context.named(name);
        }
        PropFile propFile = new PropFile(context, scope, PropFile.Loc.System);
        if (load(path, PropFileReader.Default, propFile, create, nullOnFNF)) {
            return propFile;
        } else if (nullOnFNF) {
            return null;
        } else {
            return PropFile.Empty;
        }
    }

    /**
     * Loads {@code path} into the given {@code into} argument.
     * @param path to load
     * @param propFileReader to use to load the properties file at {@code path} into {@code into}
     * @param into is the {@link PropFile} object into which to load the properties at {@code path}.
     * @param create true will create the file if it does not exist; false otherwise
     * @param printOnFNF if true, will print a message if the file is not found, otherwise nothing is printed
     * @return true on success, false otherwise
     * @throws PropFileReader.Invalid if the properties file at {@code path} is invalid
     * @see PropFileReader#load(java.io.BufferedReader, PropFile)
     */
    public static boolean load(String path, PropFileReader propFileReader, PropFile into, boolean create, boolean printOnFNF)
            throws PropFileReader.Invalid {
        if ((path == null) || (propFileReader == null) || (into == null)) {
            throw new NullPointerException("The path to load and the PropFile must not be null.");
        }
        File propertiesFile = new File(path);
        BufferedReader reader = null;
        try {
            if (create && !propertiesFile.exists()) {
                propertiesFile.getParentFile().mkdirs();
                propertiesFile.createNewFile();
            }
            reader = new BufferedReader(new FileReader(propertiesFile));
            propFileReader.load(reader, into);
            return true;
        } catch (FileNotFoundException fnfe) {
            if (printOnFNF) {
                Output.print("Cannot load properties file, %s, it does not exist.", path);
                Output.print(fnfe);
            }
        } catch (IOException ioe) {
            Output.print(ioe);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ioe) {
                    throw new AssertionError(ioe);
                }
            }
        }
        return false;
    }

    /**
     * Calls {@link #store(PropFile, PropFileWriter, String, boolean)} with {@link PropFileWriter#Default} as the
     * implementation for the {@link PropFileWriter} argument and false for the create argument.
     * @param propFile @see {@link #store(PropFile, PropFileWriter, String, boolean)}
     * @param to @see {@link #store(PropFile, PropFileWriter, String, boolean)}
     * @return true on success; false otherwise
     */
    public static boolean store(PropFile propFile, String to) {
        return store(propFile, PropFileWriter.Default, to, false);
    }

    /**
     * Calls {@link #store(PropFile, PropFileWriter, String, boolean)} with {@link PropFileWriter#Default} as the 
     * implementation for the {@link PropFileWriter} argument.
     * @param propFile @see {@link #store(PropFile, PropFileWriter, String, boolean)}
     * @param to @see {@link #store(PropFile, PropFileWriter, String, boolean)}
     * @param create @see {@link #store(PropFile, PropFileWriter, String, boolean)}
     * @return true on success; false otherwise
     */
    public static boolean store(PropFile propFile, String to, boolean create) {
        return store(propFile, PropFileWriter.Default, to, create);
    }

    /**
     * Stores {@code propFile} into {@code to} with the given {@code comment}.  If {@code create} is true
     * then {@code to} will be created if it does not exist (including necessary directories).
     * @param propFile to store
     * @param propFileWriter to use to store the {@code propFile} to {@code to}.
     * @param to which to store {@code propFile}
     * @param create the {@code to} file if it doesn't exist (including it's sub-directories).
     * @return true on success; false otherwise
     */
    public static boolean store(PropFile propFile, PropFileWriter propFileWriter, String to, boolean create) {
        if ((propFile == null) || (propFileWriter == null) || (to == null)) {
            return false;
        }
        File propertiesFile = new File(to);
        BufferedWriter writer = null;
        try {
            if (create && !propertiesFile.exists()) {
                propertiesFile.getParentFile().mkdirs();
                propertiesFile.createNewFile();
            } else if (!create && !propertiesFile.exists()) {
                return false;
            }
            writer = new BufferedWriter(new FileWriter(propertiesFile));
            propFileWriter.store(writer, propFile);
            return true;
        } catch (FileNotFoundException fnfe) {
            Output.print("Cannot load properties file, %s, it does not exist.", to);
            Output.print(fnfe);
        } catch (IOException ioe) {
            Output.print(ioe);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ioe) {
                    throw new AssertionError(ioe);
                }
            }
        }
        return false;
    }

    private PropFiles() { }

}
