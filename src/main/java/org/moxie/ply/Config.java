package org.moxie.ply;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * User: blangel
 * Date: 9/2/11
 * Time: 11:09 AM
 *
 * Handles getting and setting of global as well as project specific properties.
 * Config usage is:
 * <pre>ply config [--usage] [--global] <get|set|remove></pre>
 * where {@literal --usage} prints the usage screen and command {@literal get}
 * takes a name and prints the current value of the property and command {@literal set} takes a name and a value
 * parameter and sets the property named {@literal name} to {@literal value} within the context where the context
 * is either the current project or the global context if option {@literal --global} is present. Command {@literal remove}
 * will remove the named property from within the given context (note, not all global properties are allowed to be
 * removed).
 */
public final class Config {

    public static final class Prop {
        final String value;
        final String context;
        private Prop(String value, String context) {
            this.value = value;
            this.context = context;
        }
    }

    private static final String INSTALL_DIRECTORY = System.getProperty("ply.home");

    private static final String PLY_VERSION = System.getProperty("ply.version");

    static final Properties MANDATORY_GLOBAL_PROPS = new Properties();

    private static final Map<String, Prop> RESOLVED_PROPS = new HashMap<String, Prop>();

    public static final String CONFIG_DIR_NAME = "config";

    public static final String SCRIPTS_DIR_NAME = "scripts";

    public static final String PROP_FILE_NAME = "ply.properties";

    public static final File GLOBAL_CONFIG_DIR = new File(INSTALL_DIRECTORY + File.separator + CONFIG_DIR_NAME);

    public static final File GLOBAL_SCRIPTS_DIR = new File(INSTALL_DIRECTORY + File.separator + SCRIPTS_DIR_NAME);

    public static final File GLOBAL_PROPS_FILE = new File(INSTALL_DIRECTORY + File.separator + CONFIG_DIR_NAME + File.separator + PROP_FILE_NAME);

    public static final File LOCAL_CONFIG_DIR;

    public static final File LOCAL_PROPS_FILE;

    static {
        LOCAL_CONFIG_DIR = resolveLocalDir();
        String localConfigDirPath = LOCAL_CONFIG_DIR.getPath();
        LOCAL_PROPS_FILE = new File(localConfigDirPath + (localConfigDirPath.endsWith(File.separator) ? "" : File.separator)
                                    + PROP_FILE_NAME);
        MANDATORY_GLOBAL_PROPS.setProperty("src.dir", "src/main");
        MANDATORY_GLOBAL_PROPS.setProperty("test.src.dir", "src/test");
        MANDATORY_GLOBAL_PROPS.setProperty("build.dir", "build/main");
        MANDATORY_GLOBAL_PROPS.setProperty("test.build.dir", "build/test");
        MANDATORY_GLOBAL_PROPS.setProperty("scripts.dir", "scripts");
        MANDATORY_GLOBAL_PROPS.setProperty("init", "ply-init-" + PLY_VERSION + ".jar");
        MANDATORY_GLOBAL_PROPS.setProperty("clean", "ply-clean-" + PLY_VERSION + ".jar");
        // ensure output is loaded first
        getResolvedProperties();
        // ensure MANDATORY_GLOBAL_PROPS exist (for an individual run
        // doesn't actually matter where it comes from, local or global, a newly init-ed project will eventually
        // replace missing mandatory props in the global context)
        for (String mandatoryKey : MANDATORY_GLOBAL_PROPS.stringPropertyNames()) {
            if (!RESOLVED_PROPS.containsKey(mandatoryKey)) {
                String value = (String) MANDATORY_GLOBAL_PROPS.get(mandatoryKey);
                Output.print("^error^ mandatory property ^b^%s^r^ missing, setting ^b^%s^r^ = ^blue^%s^r^ in global.", mandatoryKey, mandatoryKey, value);
                setProperty(true, mandatoryKey, value);
            }
        }
    }
    private static File resolveLocalDir() {
        String root = "/.ply/" + CONFIG_DIR_NAME;
        String defaultPath = "./.ply/" + CONFIG_DIR_NAME;
        String path = defaultPath;
        File ply = new File(path);
        try {
            while (!ply.exists() && !root.equals(ply.getCanonicalPath())) {
                path = "../" + path;
                ply = new File(path);
            }
            if (root.equals(ply.getCanonicalPath())) {
                return new File(defaultPath);
            }
        } catch (IOException ioe) {
            Output.print(ioe);
            return new File(defaultPath);
        }
        return ply;
    }

    public static void invoke(String args[]) {
        if ((args == null) || (args.length < 2)) {
            usage();
            return;
        }
        if ("--usage".equals(args[1])) {
            usage();
        } else {
            boolean global = "--global".equals(args[1]);
            if ((args.length >= (global ? 3 : 2)) && "get".equals(args[global ? 2 : 1])) {
                // has name to retrieve
                if (args.length == (global ? 4 : 3)) {
                    printPropertyValue(args[global ? 3 : 2]);
                }
                // no name, print all
                else {
                    printPropertyValues();
                }
            } else if ((args.length == (global ? 5 : 4)) && "set".equals(args[global ? 2 : 1])) {
                setProperty(global, args[global ? 3 : 2], args[global ? 4 : 3]);
            } else if ((args.length == (global ? 4 : 3)) && "remove".equals(args[global ? 2 : 1])) {
                removeProperty(global, args[global ? 3: 2]);
            } else {
                usage();
            }
        }
    }

    public static Prop get(String name) {
        return getResolvedProperties().get(name);
    }

    static Map<String, Prop> getAllResolvedProperties() {
        return Collections.unmodifiableMap(getResolvedProperties());
    }

    public static String filter(String value) {
        if ((value == null) || (!value.contains("${"))) {
            return value;
        }
        Map<String, Prop> props = getResolvedProperties();
        for (String name : props.keySet()) {
            if (value.contains("${" + name + "}")) {
                value = value.replaceAll("\\$\\{" + name + "\\}", filter(props.get(name).value));
            }
        }
        return value;
    }

    private static void recreateGlobalPropertiesFile() {
        Config.GLOBAL_CONFIG_DIR.mkdirs();
        try {
            Config.GLOBAL_PROPS_FILE.createNewFile();
            MANDATORY_GLOBAL_PROPS.store(new FileOutputStream(Config.GLOBAL_PROPS_FILE), null);
        } catch (IOException ioe) {
            Output.print("^error^ could not create global properties file.");
            Output.print(ioe);
        }
    }

    private static void setProperty(boolean global, String name, String value) {
        if (name.contains("*")) {
            Output.print("^warn^ property names cannot contain ^b^*^r^", name);
            return;
        }
        File propertiesFile = (global ? GLOBAL_PROPS_FILE : LOCAL_PROPS_FILE);
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(propertiesFile));
            properties.setProperty(name, value);
            properties.store(new FileOutputStream(propertiesFile), null);
        } catch (FileNotFoundException fnfe) {
            Output.print("^error^ %s properties file not found!", (global ? "global" : "local"));
            Output.print(fnfe);
        } catch (IOException ioe) {
            Output.print("^error^ could not interact with %s properties file!", (global ? "global" : "local"));
            Output.print(ioe);
        }
        RESOLVED_PROPS.put(name, new Prop(value, (global ? "global" : "local")));
    }

    private static void removeProperty(boolean global, String name) {
        Map<String, Prop> resolvedProperties = getResolvedProperties();
        if (!resolvedProperties.containsKey(name)) {
            Output.print("No property ^b^%s^r^ in either local or global context.", name);
            return;
        }
        // never allow mandatory properties to be deleted from global
        if (global && MANDATORY_GLOBAL_PROPS.containsKey(name)) {
            Output.print("^warn^ cannot remove a mandatory property from global.");
            return;
        }
        resolvedProperties.remove(name);
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(global ? GLOBAL_PROPS_FILE : LOCAL_PROPS_FILE));
            properties.remove(name);
            properties.store(new FileOutputStream(global ? GLOBAL_PROPS_FILE : LOCAL_PROPS_FILE), null);
            // ensure integrity, allow deleting of mandatory overrides within local but replace with that from global
            if (!global && MANDATORY_GLOBAL_PROPS.containsKey(name)) {
                properties = new Properties();
                properties.load(new FileInputStream(GLOBAL_PROPS_FILE));
                if (properties.contains(name)) {
                    setProperty(true, name, (String) properties.get(name));
                } else {
                    setProperty(true, name, (String) MANDATORY_GLOBAL_PROPS.get(name));
                }
            }
        } catch (IOException ioe) {
            Output.print("^error^ Error interacting with file.");
            Output.print(ioe);
        }
    }

    private static void printPropertyValue(String name) {
        Map<String, Prop> properties = getResolvedProperties();
        if (name.contains("*")) {
            printPropertyValueByWildcardName(name, properties);
        } else {
            printPropertyValueByName(name, properties);
        }
    }

    private static void printPropertyValueByName(String name, Map<String, Prop> properties) {
        Prop prop = properties.get(name);
        if (prop != null) {
            Output.print("Property ^b^%s^r^ = ^cyan^%s^r^ [ ^" + ("global"
                    .equals(prop.context) ? "white" : "green") + "^%s^r^ ]", name, prop.value, prop.context);
        } else {
            Output.print("No property ^b^%s^r^ in either local or global context.", name);
        }
    }

    private static void printPropertyValueByWildcardName(String name, Map<String, Prop> properties) {
        Map<String, Prop> resolvedProps = getPropertyValuesByWildcardName(name, properties);
        if (resolvedProps.isEmpty()) {
            Output.print("No property matched ^b^%s^r^ in either local or global context.", name);
        } else {
            printPropertyValue(resolvedProps);
        }
    }

    private static Map<String, Prop> getPropertyValuesByWildcardName(String name, Map<String, Prop> properties) {
        Map<String, Prop> resolvedProps = new HashMap<String, Prop>();
        if (name.startsWith("*")) {
            name = name.substring(1);
            for (String propName : properties.keySet()) {
                if (propName.endsWith(name)) {
                    resolvedProps.put(propName, properties.get(propName));
                }
            }
        } else if (name.endsWith("*")) {
            name = name.substring(0, name.length() - 1);
            for (String propName : properties.keySet()) {
                if (propName.startsWith(name)) {
                    resolvedProps.put(propName, properties.get(propName));
                }
            }
        } else {
            String startsWithName = name.substring(0, name.indexOf("*") + 1);
            Map<String, Prop> startsWithProps = getPropertyValuesByWildcardName(startsWithName, properties);
            String endsWithName = name.substring(name.indexOf("*"));
            resolvedProps = getPropertyValuesByWildcardName(endsWithName, startsWithProps);
        }
        return resolvedProps;
    }

    private static void printPropertyValues() {
        Map<String, Prop> properties = getResolvedProperties();
        printPropertyValue(properties);
    }

    private static void printPropertyValue(Map<String, Prop> properties) {
        Output.print("Properties:");
        List<String> keys = new ArrayList<String>(properties.keySet());
        Collections.sort(keys);
        for (String key : keys) {
            Prop prop = properties.get(key);
            Output.print("\t^b^%s^r^ = ^cyan^%s^r^" + ("local".equals(prop.context) ? " ^green^*^r^" : ""), key, prop.value);
        }
        Output.print("^green^*^r^ indicates property is set within the ^b^local^r^ context.");
    }

    private static Map<String, Prop> getResolvedProperties() {
        if (!RESOLVED_PROPS.isEmpty()) {
            return RESOLVED_PROPS;
        }
        Properties globalProperties = new Properties();
        try {
            globalProperties.load(new FileInputStream(GLOBAL_PROPS_FILE));
        } catch (IOException ioe) {
            Output.print("^error^ global properties file not found!");
            Output.print(ioe);
            // auto-recover
            Output.print("^warn^ recreating global properties file.");
            recreateGlobalPropertiesFile();
            try {
                globalProperties.load(new FileInputStream(GLOBAL_PROPS_FILE));
            } catch (IOException ioe2) {
                Output.print("^error^ cannot load global properties file.");
                Output.print(ioe2);
                System.exit(1);
            }
        }
        Properties localProperties = new Properties();
        if (LOCAL_PROPS_FILE.exists()) {
            try {
                localProperties.load(new FileInputStream(LOCAL_PROPS_FILE));
            } catch (IOException ioe) {
                Output.print("^error^ local properties file not found!");
                Output.print(ioe);
            }
            for (String key : localProperties.stringPropertyNames()) {
                globalProperties.setProperty(key, (String) localProperties.get(key));
            }
        } else {
            Output.print("^error^ not a ply project (or any of the parent directories), please initialize first ^b^ply init^r^.");
            System.exit(1);
        }
        for (String key : globalProperties.stringPropertyNames()) {
            Prop prop = new Prop((String) globalProperties.get(key), (localProperties.containsKey(key) ? "local" : "global"));
            RESOLVED_PROPS.put(key, prop);
        }
        return RESOLVED_PROPS;
    }

    private static void usage() {
        Output.print("ply config [--usage] [--global] <^b^command^r^>");
        Output.print("  where ^b^command^r^ is either:");
        Output.print("    ^b^get [name]^r^\t: prints the value of the property (if not specified all properties are printed)");
        Output.print("    ^b^set <name> <value>^r^\t: sets the value of property within the context (global or local depending upon --global option presence).");
        Output.print("    ^b^remove <name>^r^\t: removes the property from the context (global or local depending upon --global option presence)");
    }

}
