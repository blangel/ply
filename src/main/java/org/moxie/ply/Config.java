package org.moxie.ply;

import org.moxie.ply.props.Prop;
import org.moxie.ply.props.Props;

import java.io.File;
import java.util.*;

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
     * The singleton holder object.
     */
    private static final class ConfigHolder {
        private static final Config INSTANCE = new Config();
        private static Config getInstance() {
            return INSTANCE;
        }
    }

    /**
     * Invokes the configuration script with the given {@code args}.
     * @param args to the configuration script
     */
    public static void invoke(String args[]) {
        get()._invoke(args);
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

    private Config() { }

    private void _invoke(String args[]) {
        if ((args == null) || (args.length < 2) || "--usage".equals(args[1])) {
            usage();
            return;
        }
        boolean explicitlyDefinedContext = args[1].startsWith("--");
        String context = explicitlyDefinedContext ? args[1].substring(2) : Props.DEFAULT_CONTEXT;
        if (explicitlyDefinedContext) {
            args = removeContext(args);
        }
        if ((args.length >= 2) && "get".equals(args[1])) {
            // has name to retrieve
            if (args.length == 3) {
                String name = args[2];
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
        } else if ((args.length == 4) && "set".equals(args[1])) {
            setProperty(context, args[2], args[3]);
        } else if ((args.length == 4) && "append".equals(args[1])) {
            appendProperty(context, args[2], args[3]);
        } else if ((args.length == 4) && "prepend".equals(args[1])) {
            prependProperty(context, args[2], args[3]);
        } else if ((args.length == 3) && "remove".equals(args[1])) {
            removeProperty(context, args[2]);
        } else {
            usage();
        }
    }

    /**
     * @param args to copy
     * @return args sans the args[1] value
     */
    private static String[] removeContext(String[] args) {
        String[] newArgs = new String[args.length - 1];
        newArgs[0] = args[0];
        System.arraycopy(args, 2, newArgs, 1, args.length - 2);
        return newArgs;
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
        Properties properties = PropertiesFileUtil.load(propertiesFile.getPath(), false, true);
        if (properties == null) {
            properties = new Properties();
        }
        properties.setProperty(name, value);
        PropertiesFileUtil.store(properties, propertiesFile.getPath(), true);
    }

    /**
     * Appends {@code value} to the property named {@code name} in context {@code context}.
     * If no existing property exists for {@code name} within {@code context} a new one will be created.
     * @param context of the property to which to append
     * @param name of the property
     * @param value to append
     */
    private void appendProperty(String context, String name, String value) {
        String existingValue = Props.getPropertiesWithCollapsedScope(context, name).get(name).value;
        if (existingValue == null) {
            existingValue = "";
        }
        setProperty(context, name, (existingValue + " " + value).trim());
    }

    /**
     * Prepends {@code value} to the property named {@code name} in context {@code context}.
     * If no existing property exists for {@code name} within {@code context} a new one will be created.
     * @param context of the property to which to prepend
     * @param name of the property
     * @param value to prepend
     */
    private void prependProperty(String context, String name, String value) {
        String existingValue = Props.getPropertiesWithCollapsedScope(context, name).get(name).value;
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
        Properties properties = PropertiesFileUtil.load(propertiesFile.getPath());
        properties.remove(name);
        if (properties.isEmpty()) {
            propertiesFile.delete();
        } else {
            PropertiesFileUtil.store(properties, propertiesFile.getPath());
        }
    }

    /**
     * Print all properties for all contexts and all scopes.
     */
    private void print() {
        print(Props.getPropertiesWithCollapsedScope());
    }

    /**
     * Print all properties matching {@code name} from within any context and any scope.
     * @param name to match (may include wildcard) from within any context and any scope.
     */
    private void print(String name) {
        Map<String, Map<String, Prop>> props = Props.getPropertiesWithCollapsedScope(name);
        if (props.isEmpty()) {
            Output.print("No property matched ^b^%s^r^ in any context.", name);
        } else {
            print(props);
        }
    }

    /**
     * Prints all properties of {@code contextDotScope}
     * @param contextDotScope the context[.scope] to print
     */
    private void printContext(String contextDotScope) {
        Map<String, Prop> contextProps = Props.getPropertiesWithCollapsedScope().get(contextDotScope);
        if (contextProps == null) {
            Output.print("No context ^b^%s^r^ found.", contextDotScope);
        } else {
            printContext(contextDotScope, contextProps);
        }
    }

    /**
     * Prints all properties matching {@code name} from context {@code context}
     * @param contextDotScope to look for properties matching {@code name} to print
     * @param name to match (may include wildcard) from within {@code context}.
     */
    private void print(String contextDotScope, String name) {
        Map<String, Prop> props = Props.getPropertiesWithCollapsedScope(contextDotScope, name);
        Map<String, Map<String, Prop>> contextDotScopeProps = new HashMap<String, Map<String, Prop>>();
        if (props.isEmpty()) {
            Output.print("No property matched ^b^%s^r^ in ^b^%s^r^.", name, contextDotScope);
        } else {
            contextDotScopeProps.put(contextDotScope, props);
            print(contextDotScopeProps);
        }
    }

    /**
     * Print all properties from within {@code props} by their context.
     * @param props mapping of context.scope to properties to print.
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
     * Prints all {@code props} coming from the context, {@code context} (including all scopes).
     * @param contextDotScope from which the {@code props} come
     * @param props to print
     * @return true if any of the properties to print are global.
     */
    private boolean printContext(String contextDotScope, Map<String, Prop> props) {
        boolean hasGlobal = false;
        Output.print("Properties from ^b^%s^r^", contextDotScope);
        List<String> propertyNames = new ArrayList<String>(props.keySet());
        Collections.sort(propertyNames);
        for (String name : propertyNames) {
            printPropertyValueByName("\t", contextDotScope, name, props);
            Prop prop = props.get(name);
            if (!prop.localOverride) {
                hasGlobal = true;
            }
        }
        return hasGlobal;
    }

    /**
     * Prints the value of property named {@code name} within context[.scope] {@code contextDotScope} for the given
     * resolved properties {@code resolvedProps}.
     * @param prefix to print before the property name and value
     * @param contextDotScope the name of the context[.scope] for which to look for a property named {@code name}.
     * @param name the name of the property to print
     * @param resolvedProps the resolved properties to look within.
     */
    private void printPropertyValueByName(String prefix, String contextDotScope, String name, Map<String, Prop> resolvedProps) {
        Prop prop = resolvedProps.get(name);
        if (prop != null) {
            Output.print("%s^b^%s^r^ = ^cyan^%s^r^ ^green^%s^r^", prefix, name, prop.value, (!prop.localOverride ? "*" : ""));
        } else {
            Output.print("No property ^b^%s^r^ in ^b^%s^r^.", name, contextDotScope);
        }
    }

    /**
     * Returns the resolved properties for use as environmental variables.  By convention, "ply$",
     * the context, the scope and the property name will be combined to form the environmental variable name.
     * The combination consists of "ply$" concatenated with the context concatenated with the scope (if any) by '#',
     * concatenated with the property name by a '.'.  For example,
     * for context 'ply', the default scope and property name 'color' the result would be 'ply$ply.color'.  If the
     * context was 'project' the scope 'test' and the property src.dir then the result would be 'ply$project#test.src.dir'
     * @return the resolved properties for use as environmental variables.
     */
    private Map<String, Prop> _getResolvedEnvironmentalProperties() {
        Map<String, Prop> environmentalProperties = Props.exportPropsToEnv();
        // now add some synthetic properties like the local ply directory location.
        environmentalProperties.put("ply$ply#project.dir", new Prop("ply", "", "project.dir", PlyUtil.LOCAL_PROJECT_DIR.getPath(), true));
        environmentalProperties.put("ply$ply#java", new Prop("ply", "", "java", System.getProperty("ply.java"), true));
        // scripts are always executed from the parent to '.ply/' directory, allow them to know where the 'ply' invocation
        // actually occurred.
        environmentalProperties.put("ply$ply#parent.user.dir", new Prop("parent", "", "user.dir", System.getProperty("user.dir"), true));
        return environmentalProperties;
    }

    private static File getContextPropertyFile(String context) {
        String localConfigPath = PlyUtil.LOCAL_CONFIG_DIR.getPath();
        return new File(localConfigPath + (localConfigPath.endsWith(File.separator) ? "" : File.separator)
                + context + ".properties");
    }

    private static void usage() {
        Output.print("ply config [--usage] [--context[.scope]] <^b^command^r^>");
        Output.print("  where ^b^command^r^ is either:");
        Output.print("    ^b^get [name]^r^\t: prints the value of the property (if not specified all properties are printed)");
        Output.print("    ^b^set <name> <value>^r^\t: sets the value of property within the context[.scope].");
        Output.print("    ^b^append <name> <value>^r^\t: appends the value to property within the context[.scope].");
        Output.print("    ^b^prepend <name> <value>^r^\t: prepends the value to property within the context[.scope].");
        Output.print("    ^b^remove <name>^r^\t: removes the property from the context[.scope].");
        Output.print("  the default context is ^b^ply^r^ and the default scope is null.");
    }

}