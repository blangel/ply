package net.ocheyedan.ply;

import net.ocheyedan.ply.props.Prop;
import net.ocheyedan.ply.props.Props;

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
        if ((args.length >= 2) && ("get".equals(args[1]) || "get-all".equals(args[1]))) {
            boolean justLocal = "get".equals(args[1]);
            // has name to retrieve
            if (args.length == 3) {
                String name = args[2];
                if (!explicitlyDefinedContext) {
                    print(name, justLocal);
                } else {
                    print(context, name, justLocal);
                }
            }
            // no name, but has context
            else if (explicitlyDefinedContext) {
                printContext(context, justLocal);
            }
            // no name, print all
            else {
                print(justLocal);
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
     * @param context of the property to set
     * @param name the name of the property to set
     * @param value the value of the property to set
     */
    private void setProperty(String context, String name, String value) {
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
        String existingValue = Props.getValue(context, name);
        if (existingValue == null) {
            existingValue = "";
        } else if (!existingValue.isEmpty()) {
            existingValue = existingValue + " ";
        }
        setProperty(context, name, (existingValue + value).trim());
    }

    /**
     * Prepends {@code value} to the property named {@code name} in context {@code context}.
     * If no existing property exists for {@code name} within {@code context} a new one will be created.
     * @param context of the property to which to prepend
     * @param name of the property
     * @param value to prepend
     */
    private void prependProperty(String context, String name, String value) {
        String existingValue = Props.getValue(context, name);
        if (existingValue == null) {
            existingValue = "";
        } else if (!existingValue.isEmpty()) {
            existingValue = " " + existingValue;
        }
        setProperty(context, name, (value + existingValue).trim());
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
     * @param justLocal true if only the local properties are to be printed
     */
    private void print(boolean justLocal) {
        print(Props.getProps(), justLocal);
    }

    /**
     * Print all properties matching {@code name} from within any context and any scope.
     * @param name to match (may include wildcard) from within any context and any scope.
     * @param justLocal true if only the local properties are to be printed
     */
    private void print(String name, boolean justLocal) {
        Map<String, Map<String, Prop>> matches = new HashMap<String, Map<String, Prop>>();
        for (String context : Props.getProps().keySet()) {
            Map<String, Prop> matchedProps = Props.getProps(context, name);
            if ((matchedProps != null) && !matchedProps.isEmpty()) {
                matches.put(context, matchedProps);
            }
        }
        if (matches.isEmpty()) {
            Output.print("No property matched ^b^%s^r^ in any context.", name);
        } else {
            print(matches, justLocal);
        }
    }

    /**
     * Prints all properties of {@code contextDotScope}
     * @param contextDotScope the context[.scope] to print
     * @param justLocal true if only the local properties are to be printed
     */
    private void printContext(String contextDotScope, boolean justLocal) {
        Map<String, Prop> contextProps = Props.getProps(contextDotScope);
        if (contextProps == null) {
            Output.print("No context ^b^%s^r^ found.", contextDotScope);
        } else {
            printContext(contextDotScope, contextProps, justLocal);
        }
    }

    /**
     * Prints all properties matching {@code name} from context {@code context}
     * @param contextDotScope to look for properties matching {@code name} to print
     * @param name to match (may include wildcard) from within {@code context}.
     * @param justLocal true if only the local properties are to be printed
     */
    private void print(String contextDotScope, String name, boolean justLocal) {
        Map<String, Prop> props = Props.getProps(contextDotScope, name);
        Map<String, Map<String, Prop>> contextDotScopeProps = new HashMap<String, Map<String, Prop>>();
        if ((props == null) || props.isEmpty()) {
            Output.print("No property matched ^b^%s^r^ in ^b^%s^r^.", name, contextDotScope);
        } else {
            contextDotScopeProps.put(contextDotScope, props);
            print(contextDotScopeProps, justLocal);
        }
    }

    /**
     * Print all properties from within {@code props} by their context.
     * @param props mapping of context.scope to properties to print.
     * @param justLocal true if only the local properties are to be printed
     */
    private void print(Map<String, Map<String, Prop>> props, boolean justLocal) {
        boolean hasGlobal = false;
        List<String> contexts = new ArrayList<String>(props.keySet());
        Collections.sort(contexts);
        for (String context : contexts) {
            if (printContext(context, props.get(context), justLocal)) {
                hasGlobal = true;
            }
        }
        if (hasGlobal && !justLocal) {
            Output.print("^green^*^r^ indicates system-wide property.");
        }
    }

    /**
     * Prints all {@code props} coming from the context, {@code context} (including all scopes).
     * @param contextDotScope from which the {@code props} come
     * @param props to print
     * @param justLocal true if only the local properties are to be printed
     * @return true if any of the properties to print are global.
     */
    private boolean printContext(String contextDotScope, Map<String, Prop> props, boolean justLocal) {
        boolean hasGlobal = false, printedHeader = false;
        List<String> propertyNames = new ArrayList<String>(props.keySet());
        Collections.sort(propertyNames);
        for (String name : propertyNames) {
            Prop prop = props.get(name);
            if (!prop.localOverride && !justLocal) {
                hasGlobal = true;
                if (!printedHeader) {
                    Output.print("Properties from ^b^%s^r^", contextDotScope);
                    printedHeader = true;
                }
                printPropertyValueByName("\t", contextDotScope, name, props);
            } else if (prop.localOverride) {
                if (!printedHeader) {
                    Output.print("Properties from ^b^%s^r^", contextDotScope);
                    printedHeader = true;
                }
                printPropertyValueByName("\t", contextDotScope, name, props);
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

    private static File getContextPropertyFile(String context) {
        String localConfigPath = PlyUtil.LOCAL_CONFIG_DIR.getPath();
        return new File(localConfigPath + (localConfigPath.endsWith(File.separator) ? "" : File.separator)
                + context + ".properties");
    }

    private static void usage() {
        Output.print("ply config [--usage] [--context[.scope]] <^b^command^r^>");
        Output.print("  where ^b^command^r^ is either:");
        Output.print("    ^b^get [name]^r^             : prints the value of the property (if not specified all properties are printed)");
        Output.print("    ^b^get-all [name]^r^         : like ^b^get^r^ but prints system properties as well.");
        Output.print("    ^b^set <name> <value>^r^     : sets the value of property within the context[.scope].");
        Output.print("    ^b^append <name> <value>^r^  : appends the value to property within the context[.scope].");
        Output.print("    ^b^prepend <name> <value>^r^ : prepends the value to property within the context[.scope].");
        Output.print("    ^b^remove <name>^r^          : removes the property from the context[.scope].");
        Output.print("  the default context is ^b^ply^r^ and the default scope is null.");
    }

}