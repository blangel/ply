package org.moxie.ply;

import javax.management.StringValueExp;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * User: blangel
 * Date: 9/2/11
 * Time: 11:09 AM
 *
 * Handles getting global and local properties as well as setting of local properties.
 * Config usage is:
 * <pre>ply config [--usage] [--context] <get|set|remove|append|prepend></pre>
 * where {@literal --usage} prints the usage screen and command {@literal get}
 * takes a name and prints the current value of the property for the given {@literal context}. Command {@literal set}
 * takes a name and a value parameter and sets the property named {@literal name} to {@literal value} within the context.
 * Command {@literal remove} will remove the named property from within the given context.
 * Command {@literal append} will append the value to the named property's existing value (or create new if a value
 * doesn't exist) within the given context. Command {@literal prepend} will prepend the value to the named property's
 * existing value (or create new if a value doesn't exist) within the given context.
 * For the about operations, if no context is given then the default {@literal ply} is assumed).
 */
public final class Config {

    /**
     * A representation of a single property including the context from which it came and whether
     * it is a local override.
     */
    static final class Prop {
        final String name;
        final String value;
        final String context;
        final boolean localOverride;
        Prop(String context, String name, String value, boolean localOverride) {
            this.context = context;
            this.name = name;
            this.value = value;
            this.localOverride = localOverride;
        }
    }

    /**
     * The singleton holder object.
     */
    private static final class ConfigHolder {
        private static final Config INSTANCE = new Config();
        private static Config getInstance() {
            return INSTANCE;
        }
    }

    /**
     * A {@link FilenameFilter} for {@link Properties} files.
     */
    private static final FilenameFilter PROPERTIES_FILENAME_FILTER = new FilenameFilter() {
        @Override public boolean accept(File dir, String name) {
            return name.endsWith(".properties");
        }
    };

    /**
     * The directory in which ply is installed, passed in by the invoking script.
     */
    private static final String INSTALL_DIRECTORY = System.getProperty("ply.home");

    /**
     * The current version of the ply program, passed in by the invoking script.
     */
    private static final String PLY_VERSION = System.getProperty("ply.version");

    /**
     * When no context is specified, this context will be used.
     */
    public static final String DEFAULT_CONTEXT = "ply";

    /**
     * The configuration directory (in which property files are stored) for the install.
     */
    public static final File GLOBAL_CONFIG_DIR = new File(INSTALL_DIRECTORY + File.separator + "config");

    /**
     * The scripts directory for the install.
     */
    public static final File GLOBAL_SCRIPTS_DIR = new File(INSTALL_DIRECTORY + File.separator + "scripts");

    /**
     * The local project directory (local to the init-ed project).
     */
    public static final File LOCAL_PROJECT_DIR = resolveLocalDir();

    /**
     * The local configuration directory (local to the init-ed project).
     */
    public static final File LOCAL_CONFIG_DIR = resolveLocalConfigDir(LOCAL_PROJECT_DIR);

    /**
     * Invokes the configuration script with the given {@code args}.
     * @param args to the configuration script
     */
    public static void invoke(String args[]) {
        get()._invoke(args);
    }

    /**
     * @param context in which to get {@code name}
     * @param name of the property to get
     * @return the value for the property named {@code name} within context {@code context} or null if none exists.
     */
    public static String get(String context, String name) {
        return get()._get(context, name);
    }

    /**
     * @param context in which to get {@code name}
     * @param name of the property to get
     * @param filter true if the value should be filtered
     * @return the value for the property named {@code name} within context {@code context} or null if none exists.
     */
    public static String get(String context, String name, boolean filter) {
        return get()._get(context, name, filter);
    }

    /**
     * @param context to retrieve
     * @return the properties within {@code context}
     */
    public static Map<String, Prop> getContext(String context) {
        return get()._getContext(context);
    }

    /**
     * @param name of the property to get
     * @return the value for the property named {@code name} within the default context, {@link #DEFAULT_CONTEXT},
     *         or null if none exists.
     */
    public static String get(String name) {
        return get()._get(name);
    }

    /**
     * @param name of the property to get
     * @param filter true if the property value should be filtered
     * @return the value for the property named {@code name} within the default context, {@link #DEFAULT_CONTEXT},
     *         or null if none exists.
     */
    public static String get(String name, boolean filter) {
        return get()._get(name, filter);
    }

    /**
     * Filters {@code value} by resolving all unix-style properties defined within against the resolved properties
     * of this configuration.
     * @param value to filter
     * @return the filtered value
     */
    public static String filter(Prop value) {
        return get()._filter(value);
    }

    /**
     * Returns the resolved properties for use as environmental variables.  By convention,
     * the context and the property name will be combined to form the environmental variable name.
     * The combination consists of the context concatenated with the property name by a '.'.  For example,
     * for context 'ply' and property name 'color' the concatenation would be 'ply.color'.
     * @return the resolved properties for use as environmental variables.
     */
    static Map<String, Prop> getResolvedEnvironmentalProperties() {
        return get()._getResolvedEnvironmentalProperties();
    }

    /**
     * @return the singleton instance of this class.
     */
    private static Config get() {
        return ConfigHolder.getInstance();
    }

    /**
     * This directory has to be resolved as ply can be invoked from within a nested directory.
     * @return the resolved local ply project directory
     */
    private static File resolveLocalDir() {
        String root = "/.ply";
        String defaultPath = "./.ply";
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

    private static File resolveLocalConfigDir(File resolvedLocalProjectDir) {
        try {
            String path = resolvedLocalProjectDir.getCanonicalPath();
            return new File(path.endsWith(File.separator) ? path + "config" : path + File.separator + "config");
        } catch (IOException ioe) {
            Output.print(ioe);
            return new File(resolvedLocalProjectDir.getPath() + "config");
        }
    }

    /**
     * A mapping of context name to a mapping of property name to {@link Prop} object.
     */
    private final Map<String, Map<String, Prop>> contextToResolvedProperty = new HashMap<String, Map<String, Prop>>();

    /**
     * A mapping of pre-filtered values to filtered-values.
     */
    private final Map<String, String> filteredCache = new HashMap<String, String>();

    /**
     * Flag to indicate if the properties map has been initialized yet.
     */
    private final AtomicBoolean hasBeenResolved = new AtomicBoolean(false);

    private Config() { }

    private void _invoke(String args[]) {
        if ((args == null) || (args.length < 2) || "--usage".equals(args[1])) {
            usage();
            return;
        }
        boolean explicitlyDefinedContext = args[1].startsWith("--");
        String context = explicitlyDefinedContext ? args[1].substring(2) : DEFAULT_CONTEXT;
        if ((args.length >= (explicitlyDefinedContext ? 3 : 2))
                && "get".equals(args[explicitlyDefinedContext ? 2 : 1])) {
            resolveProperties();
            // has name to retrieve
            if (args.length == (explicitlyDefinedContext ? 4 : 3)) {
                String name = args[explicitlyDefinedContext ? 3 : 2];
                if (!explicitlyDefinedContext) {
                    print(name);
                } else {
                    print(context, name);
                }
            }
            // no name, but has context
            else if (explicitlyDefinedContext) {
                printContext(context);
            }
            // no name, print all
            else {
                print();
            }
        } else if ((args.length == (explicitlyDefinedContext ? 5 : 4))
                && "set".equals(args[explicitlyDefinedContext ? 2 : 1])) {
            setProperty(context, args[explicitlyDefinedContext ? 3 : 2], args[explicitlyDefinedContext ? 4 : 3]);
        } else if ((args.length == (explicitlyDefinedContext ? 5 : 4))
                && "append".equals(args[explicitlyDefinedContext ? 2 : 1])) {
            appendProperty(context, args[explicitlyDefinedContext ? 3 : 2], args[explicitlyDefinedContext ? 4 : 3]);
        } else if ((args.length == (explicitlyDefinedContext ? 5 : 4))
                && "prepend".equals(args[explicitlyDefinedContext ? 2 : 1])) {
            prependProperty(context, args[explicitlyDefinedContext ? 3 : 2], args[explicitlyDefinedContext ? 4 : 3]);
        } else if ((args.length == (explicitlyDefinedContext ? 4 : 3))
                && "remove".equals(args[explicitlyDefinedContext ? 2 : 1])) {
            removeProperty(context, args[explicitlyDefinedContext ? 3: 2]);
        } else {
            usage();
        }
    }

    /**
     * @param context in which to get {@code name}
     * @param name of the property to get
     * @return the value for the property named {@code name} within context {@code context} or null if none exists.
     */
    private String _get(String context, String name) {
        return _get(context, name, false);
    }

    /**
     * @param context in which to get {@code name}
     * @param name of the property to get
     * @param filter true if the property value should be filtered
     * @return the value for the property named {@code name} within context {@code context} or null if none exists.
     */
    private String _get(String context, String name, boolean filter) {
        resolveProperties();
        Map<String, Prop> props = contextToResolvedProperty.get(context);
        Prop prop = (props == null ? null : props.get(name));
        return (prop == null ? null : filter ? filter(prop) : prop.value);
    }

    /**
     * @param context to retrieve
     * @return all resolved property mapping for {@code context}
     */
    private Map<String, Prop> _getContext(String context) {
        return contextToResolvedProperty.get(context);
    }

    /**
     * @param name of the property to get
     * @return the value for the property named {@code name} within the default context, {@link #DEFAULT_CONTEXT},
     *         or null if none exists.
     */
    private String _get(String name) {
        return _get(DEFAULT_CONTEXT, name);
    }

    /**
     * @param name of the property to get
     * @param filter true if the property value should be filtered
     * @return the value for the property named {@code name} within the default context, {@link #DEFAULT_CONTEXT},
     *         or null if none exists.
     */
    private String _get(String name, boolean filter) {
        return _get(DEFAULT_CONTEXT, name, filter);
    }

    /**
     * Filters {@code value} by resolving all unix-style properties defined within against the resolved properties
     * of this configuration as well as the system environment variables (to capture things like {@literal PLY_HOME}).
     * @param value to filter
     * @return the filtered value
     */
    private String _filter(Prop value) {
        if ((value == null) || (!value.value.contains("${"))) {
            return (value == null ? null : value.value);
        }
        if (filteredCache.containsKey(value.value)) {
            return filteredCache.get(value.value);
        }
        resolveProperties();
        String filtered = value.value;
        // first attempt to resolve via the value's context.
        filtered = filterBy(filtered, "", contextToResolvedProperty.get(value.context));
        // also attempt to filter context-prefixed values
        for (String context : contextToResolvedProperty.keySet()) {
            filtered = filterBy(filtered, context + ".", contextToResolvedProperty.get(context));
        }
        for (String enivronmentProperty : System.getenv().keySet()) {
            if (filtered.contains("${" + enivronmentProperty + "}")) {
                filtered = filtered.replaceAll("\\$\\{" + enivronmentProperty.replaceAll("\\.", "\\\\.") + "\\}",
                                               System.getenv(enivronmentProperty));
            }
        }
        Output.print("^dbug^ filtered ^b^%s^r^ to ^b^%s^r^.", value.value, filtered);
        filteredCache.put(value.value, filtered);
        return filtered;
    }

    private static String filterBy(String value, String prefix, Map<String, Prop> props) {
        for (String name : props.keySet()) {
            String toFind = prefix + name;
            if (value.contains("${" + toFind + "}")) {
                value = value.replaceAll("\\$\\{" + toFind.replaceAll("\\.", "\\\\.") + "\\}",
                                               filter(props.get(name)));
            }
        }
        return value;
    }

    /**
     * Sets property named {@code name} to {@code value} within context {@code context}.
     * Note, property names containing '*' are not allowed.
     * @param context of the property to set
     * @param name the name of the property to set
     * @param value the value of the property to set
     */
    private void setProperty(String context, String name, String value) {
        if (name.contains("*")) {
            Output.print("^warn^ property names cannot contain ^b^*^r^", name);
            return;
        }
        File propertiesFile = getContextPropertyFile(context);
        Properties properties = PropertiesUtil.load(propertiesFile.getPath());
        properties.setProperty(name, value);
        PropertiesUtil.store(properties, propertiesFile.getPath(), true);
        // if already resolved, keep in sync.
        if (hasBeenResolved.get()) {
            Map<String, Prop> contextProps = contextToResolvedProperty.get(context);
            if (contextProps == null) {
                contextProps = new HashMap<String, Prop>();
                contextToResolvedProperty.put(context, contextProps);
            }
            contextProps.put(name, new Prop(context, name, value, true));
        }
    }

    /**
     * Appends {@code value} to the property named {@code name} in context {@code context}.
     * If no existing property exists for {@code name} within {@code context} a new one will be created.
     * @param context of the property to which to append
     * @param name of the property
     * @param value to append
     */
    private void appendProperty(String context, String name, String value) {
        String existingValue = get(context, name);
        if (existingValue == null) {
            existingValue = "";
        }
        setProperty(context, name, (existingValue + " " + value).trim());
    }

    /**
     * Prepends {@code value} to the property named {@code name} in context {@code context}.
     * If no existing property exists for {@code name} within {@code context} a new one will be created.
     * @param context of the property to which to append
     * @param name of the property
     * @param value to append
     */
    private void prependProperty(String context, String name, String value) {
        String existingValue = get(context, name);
        if (existingValue == null) {
            existingValue = "";
        }
        setProperty(context, name, (value + " " + existingValue).trim());
    }

    /**
     * Removes property named {@code name} from context {@code context}.
     * @param context of the property to remove
     * @param name the name of the property to remove
     */
    private void removeProperty(String context, String name) {
        File propertiesFile = getContextPropertyFile(context);
        if (!propertiesFile.exists()) {
            Output.print("No property ^b^%s^r^ in context ^b^%s^r^.", name, context);
            return;
        }
        Properties properties = PropertiesUtil.load(propertiesFile.getPath());
        properties.remove(name);
        if (properties.isEmpty()) {
            propertiesFile.delete();
        } else {
            PropertiesUtil.store(properties, propertiesFile.getPath());
        }
        // if already resolved, keep in sync.
        if (hasBeenResolved.get()) {
            Map<String, Prop> contextProps = contextToResolvedProperty.get(context);
            if (contextProps != null) {
                contextProps.remove(name);
            }
        }
    }

    /**
     * Print all properties for all contexts.
     */
    private void print() {
        print(getProperties());
    }

    /**
     * Print all properties matching {@code name} from within any context.
     * @param name to match (may include wildcard) from within any context.
     */
    private void print(String name) {
        Map<String, Map<String, Prop>> props = getProperties(name);
        if (props.isEmpty()) {
            Output.print("No property matched ^b^%s^r^ in any context.", name);
        } else {
            print(props);
        }
    }

    private void printContext(String context) {
        if (!contextToResolvedProperty.containsKey(context)) {
            Output.print("No context ^b^%s^r^ found.", context);
        } else {
            printContext(context, contextToResolvedProperty.get(context));
        }
    }

    /**
     * Prints all properties matching {@code name} from context {@code context}
     * @param context to look for properties matching {@code name} to print
     * @param name to match (may include wildcard) from within {@code context}.
     */
    private void print(String context, String name) {
        Map<String, Map<String, Prop>> props = getProperties(context, name);
        if (props.isEmpty()) {
            Output.print("No property matched ^b^%s^r^ in context ^b^%s^r^.", name, context);
        } else {
            print(props);
        }
    }

    /**
     * Print all contexts properties from within {@code props}
     * @param props mapping of context to properties to print.
     */
    private void print(Map<String, Map<String, Prop>> props) {
        boolean hasGlobal = false;
        List<String> contexts = new ArrayList<String>(props.keySet());
        Collections.sort(contexts);
        for (String context : contexts) {
            if (printContext(context, props.get(context))) {
                hasGlobal = true;
            }
        }
        if (hasGlobal) {
            Output.print("^green^*^r^ indicates system-wide property.");
        }
    }

    /**
     * Prints all {@code props} coming from the context, {@code context}.
     * @param context from which the {@code props} come
     * @param props to print
     * @return true if any of the properties to print are global.
     */
    private boolean printContext(String context, Map<String, Prop> props) {
        boolean hasGlobal = false;
        Output.print("Properties from ^b^%s^r^", context);
        List<String> propertyNames = new ArrayList<String>(props.keySet());
        Collections.sort(propertyNames);
        for (String name : propertyNames) {
            printPropertyValueByName("\t", context, name, props);
            Prop prop = props.get(name);
            if (!prop.localOverride) {
                hasGlobal = true;
            }
        }
        return hasGlobal;
    }

    /**
     * Prints the value of property named {@code name} within context {@code context} for the given
     * resolved properties {@code resolvedProps}.
     * @param prefix to print before the property name and value
     * @param context the name of the context for which to look for a property named {@code name}.
     * @param name the name of the property to print
     * @param resolvedProps the resolved properties to look within.
     */
    private void printPropertyValueByName(String prefix, String context, String name, Map<String, Prop> resolvedProps) {
        Prop prop = resolvedProps.get(name);
        if (prop != null) {
            Output.print("%s^b^%s^r^ = ^cyan^%s^r^ ^green^%s^r^", prefix, name, prop.value, (!prop.localOverride ? "*" : ""));
        } else {
            Output.print("No property ^b^%s^r^ in context ^b^%s^r^.", name, context);
        }
    }

    /**
     * @return all resolved properties from all contexts.
     */
    private Map<String, Map<String, Prop>> getProperties() {
        return Collections.unmodifiableMap(contextToResolvedProperty);
    }

    /**
     * @param name to match (may include a wildcard) within all contexts.
     * @return a mapping of contexts to all their properties which match {@code name}
     */
    private Map<String, Map<String, Prop>> getProperties(String name) {
        Map<String, Map<String, Prop>> resolved = new HashMap<String, Map<String, Prop>>();
        for (String context : contextToResolvedProperty.keySet()) {
            Map<String, Map<String, Prop>> props = getProperties(context, name);
            if (!props.isEmpty() && (props.get(context) != null) && !props.get(context).isEmpty()) {
                resolved.put(context, props.get(context));
            }
        }
        return resolved;
    }

    /**
     * @param context of the properties file to look within
     * @param name to match (may include a wildcard).
     * @return a mapping of {@code context} to all properties within the context which match {@code name}
     */
    private Map<String, Map<String, Prop>> getProperties(String context, String name) {
        Map<String, Prop> props = contextToResolvedProperty.get(context);
        if (props == null) {
            Output.print("No context ^b^%s^r^ found.", context);
            System.exit(1);
        }

        Map<String, Map<String, Prop>> resolved = new HashMap<String, Map<String, Prop>>(1);
        Map<String, Prop> resolvedProps;
        if (name.contains("*")) {
            resolvedProps = getPropertyValuesByWildcardName(name, props);
        } else {
            resolvedProps = new HashMap<String, Prop>(1);
            if (props.containsKey(name)) {
                resolvedProps.put(name, props.get(name));
            }
        }
        resolved.put(context, resolvedProps);
        return resolved;
    }

    /**
     * Returns the resolved properties for use as environmental variables.  By convention,
     * the context and the property name will be combined to form the environmental variable name.
     * The combination consists of the context concatenated with the property name by a '.'.  For example,
     * for context 'ply' and property name 'color' the concatenation would be 'ply.color'.
     * @return the resolved properties for use as environmental variables.
     */
    private Map<String, Prop> _getResolvedEnvironmentalProperties() {
        resolveProperties();
        Map<String, Prop> environmentalProperties = new HashMap<String, Prop>();
        for (String context : contextToResolvedProperty.keySet()) {
            Map<String, Prop> contextProps = contextToResolvedProperty.get(context);
            for (String name : contextProps.keySet()) {
                environmentalProperties.put(context + "." + name, contextProps.get(name));
            }
        }
        // now add some synthetic properties like the local ply directory location.
        environmentalProperties.put("ply.project.dir", new Prop("ply", "project.dir", LOCAL_PROJECT_DIR.getPath(), true));
        environmentalProperties.put("ply.java", new Prop("ply", "java", System.getProperty("ply.java"), true));
        // scripts are always executed from the parent to '.ply/' directory, allow them to know where the 'ply' invocation
        // actually occurred.
        environmentalProperties.put("parent.user.dir", new Prop("parent", "user.dir", System.getProperty("user.dir"), true));
        return environmentalProperties;
    }

    /**
     * Resolves the properties from the global and project-local property files.
     * @throws IllegalStateException if the local properties directory cannot be found.
     */
    private void resolveProperties() {
        if (hasBeenResolved.getAndSet(true)) {
            return;
        }
        // first add the properties from the install directory.
        if (!GLOBAL_CONFIG_DIR.exists()) {
            Output.print("^error^ the ply install directory is corrupt, please re-install.");
            System.exit(1);
        }
        resolvePropertiesFromDirectory(GLOBAL_CONFIG_DIR, false);
        // now override with the local project's config directory.
        if (LOCAL_CONFIG_DIR.exists()) {
            resolvePropertiesFromDirectory(LOCAL_CONFIG_DIR, true);
        } else {
            throw new IllegalStateException("^warn^ not a ply project (or any of the parent directories), please initialize first ^b^ply init^r^.");
        }
    }

    /**
     * Iterates over the property files within {@code fromDirectory} and calls
     * {@link #resolvePropertiesFromFile(String, Properties, boolean)} on each (provided the file is not a directory).
     * @param fromDirectory the directory from which to resolve properties.
     * @param local true if the {@code fromDirectory} is the local configuration directory
     * @see {@link #PROPERTIES_FILENAME_FILTER}
     */
    private void resolvePropertiesFromDirectory(File fromDirectory, boolean local) {
        for (File subFile : fromDirectory.listFiles(PROPERTIES_FILENAME_FILTER)) {
            if (!subFile.isDirectory()) {
                String fileName = subFile.getName();
                int index = fileName.indexOf(".properties");
                if (index == -1) {
                    Output.print("^error^ Properties file name filter accepted file which doesn't end in ^b^.properties^r^.");
                    continue;
                }
                String context = fileName.substring(0, index);
                Properties properties = PropertiesUtil.load(subFile.getPath());
                resolvePropertiesFromFile(context, properties, local);
            }
        }
    }

    /**
     * Loads the properties from {@code properties} into the {@link #contextToResolvedProperty} mapping for {@code context}
     * @param context associated with {@code properties}
     * @param properties the loaded properties file
     * @param local true if the {@code properties} is from the local configuration directory
     */
    private void resolvePropertiesFromFile(String context, Properties properties, boolean local) {
        Map<String, Prop> contextProperties = contextToResolvedProperty.get(context);
        if (contextProperties == null) {
            contextProperties = new HashMap<String, Prop>(properties.size(), 1.0f);
            contextToResolvedProperty.put(context, contextProperties);
        }
        for (String propertyName : properties.stringPropertyNames()) {
            contextProperties.put(propertyName, new Prop(context, propertyName, properties.getProperty(propertyName), local));
        }
    }

    private static File getContextPropertyFile(String context) {
        String localConfigPath = LOCAL_CONFIG_DIR.getPath();
        return new File(localConfigPath + (localConfigPath.endsWith(File.separator) ? "" : File.separator)
                + context + ".properties");
    }

    private static void usage() {
        Output.print("ply config [--usage] [--context] <^b^command^r^>");
        Output.print("  where ^b^command^r^ is either:");
        Output.print("    ^b^get [name]^r^\t: prints the value of the property (if not specified all properties are printed)");
        Output.print("    ^b^set <name> <value>^r^\t: sets the value of property within the context.");
        Output.print("    ^b^append <name> <value>^r^\t: appends the value to property within the context");
        Output.print("    ^b^prepend <name> <value>^r^\t: prepends the value to property within the context");
        Output.print("    ^b^remove <name>^r^\t: removes the property from the context");
        Output.print("  the default context is ^b^ply^r^");
    }

    /**
     * Resolves the wildcard references within {@code name} creating a new map of matching properties from the
     * given {@code properties}.  The returned map will be a subset of {@code properties}.
     * @param name with wildcard references which need to be resolved.
     * @param properties from which to resolve
     * @return the resolved map of properties matching the wildcard-ed {@code name}
     */
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

}