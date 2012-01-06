package net.ocheyedan.ply.props;

import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.PlyUtil;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * User: blangel
 * Date: 10/6/11
 * Time: 10:13 AM
 *
 * Resolves properties either from the environment variables (if being invoked from a script executed by the ply
 * program) or from the project's property files (if being invoked from the ply program itself).
 */
public class Props {

    /**
     * The default context to use if none is specified.
     */
    public static final String DEFAULT_CONTEXT = "ply";

    /**
     * The lazily loaded singleton class/object responsible for loading the properties and making the properties
     * accessible to the static methods provided by the {@link Props} class.
     */
    private static final class Singleton {

        static final Map<String, Map<String, Prop>> PROPS = new HashMap<String, Map<String, Prop>>();

        /**
         * A cache of unfiltered values to filtered values.
         */
        private static final Map<String, String> FILTER_CACHE = new HashMap<String, String>();

        static {
            // determine if resolution needs to be done via file-system or has been passed via env-properties.
            // TODO - is this the best way? maybe use a more explicit property
            if (System.getenv("ply$ply.log.levels") != null) {
                initPropsByEnv();
            } else {
                initPropsbyFileSystem();
            }
        }

        /**
         * Delegates loading of properties from the file system to the {@link Loader#loadProjectProps()}
         */
        static void initPropsbyFileSystem() {
            PROPS.putAll(Loader.loadProjectProps());
        }

        /**
         * Populates {@link #PROPS} by extracting variables from the environment.  A variable is included
         * in the {@link #PROPS} if its key is prefixed with {@literal ply$}.  It then must conform to the format:
         * context.propertyName
         * Thus the minimal length of a ply environment key is 7; 4 for the 'ply$' prefix
         * one for the context, one for the period and one for the property name.
         */
        static void initPropsByEnv() {
            Map<String, String> env = System.getenv();
            for (String key : env.keySet()) {
                if (!key.startsWith("ply$")) {
                    continue; // non-ply property
                }
                String propertyValue = env.get(key);
                key = key.substring(4); // strip ply$
                // extract context
                int index = key.indexOf("."); // error if -1; must have been set by Ply itself
                String context = key.substring(0, index);
                key = key.substring(index + 1); // error if (length == index + 1) as property name's are non-null
                // get the context map
                Map<String, Prop> contextProps = PROPS.get(context);
                if (contextProps == null) {
                    contextProps = new HashMap<String, Prop>();
                    PROPS.put(context, contextProps);
                }
                // from ply itself there is never a scope as it is resolved and exported as the default
                contextProps.put(key, new Prop(context, "", key, propertyValue, null));
            }
        }

        /**
         * Retrieves the property associated with {@code propertyName} within {@code context}.
         * @param context for which to look for {@code propertyName}
         * @param propertyName for which to retrieve the {@link Prop}
         * @return the {@link Prop} named {@code propertyName} within context {@code context} or null if no such property
         *         exists
         */
        static Prop get(String context, String propertyName) {
            Map<String, Prop> contextProps = PROPS.get(context);
            if (contextProps == null) {
                return null;
            }
            return contextProps.get(propertyName);
        }

        /**
         * @param context for which to look for {@code propertyName}
         * @param propertyName for which to retrieve the value
         * @return the property's value or the empty string
         * @see {@link Props#getValue(String, String)}
         */
        static String getValue(String context, String propertyName) {
            Prop prop = get(context, propertyName);
            return (prop == null ? "" : prop.value);
        }

        /**
         * @return a mapping of context to its mapping of properties
         */
        static Map<String, Map<String, Prop>> getProps() {
            return Collections.unmodifiableMap(PROPS);
        }

        /**
         * @param context from which to retrieve all properties
         * @return a mapping of all property names to properties from within {@code context}, or null if no such context
         *         exists
         */
        static Map<String, Prop> getProps(String context) {
            return (PROPS.containsKey(context) ? Collections.unmodifiableMap(PROPS.get(context)) : null);
        }

        /**
         * Note, the returned mapping may have one item if {@code propertyNameLike} was directly matched
         * or only one property name matched the wildcard-ed name.
         * @param context from which to retrieve all properties which match {@code propertyNameLike}
         * @param propertyNameLike the property name (which may contain wildcards)
         * @return a mapping of all property names to properties from within {@code context} which match
         *         {@code propertyNameLike} or null if nothing is found.
         */
        @SuppressWarnings("serial")
        static Map<String, Prop> getProps(final String context, final String propertyNameLike) {
            if (!propertyNameLike.contains("*")) {
                final Map<String, Prop> contextProps = getProps(context);
                return (contextProps == null || !contextProps.containsKey(propertyNameLike) ? null
                        : new HashMap<String, Prop>(1) {{ put(propertyNameLike, contextProps.get(propertyNameLike)); }});
            }
            return getPropertyValuesByWildcardName(propertyNameLike, getProps(context));
        }

        /**
         * Resolves the wildcard references within {@code name} creating a new map of matching properties from the
         * given {@code properties}.  The returned map will be a subset of {@code properties}.
         * @param name with wildcard references which need to be resolved.
         * @param properties from which to resolve
         * @return the resolved map of properties matching the wildcard-ed {@code name}
         */
        static Map<String, Prop> getPropertyValuesByWildcardName(String name, Map<String, Prop> properties) {
            Map<String, Prop> resolvedProps = new HashMap<String, Prop>();
            if (properties == null) {
                return null;
            }
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

        /**
         * Filters {@code value} by resolving all unix-style properties defined within against the resolved properties
         * of this configuration as well as the system environment variables (to capture things like {@literal PLY_HOME}).
         * @param value to filter
         * @return the filtered value
         */
        static String filter(Prop value) {
            String localDir = "";
            try {
                localDir = PlyUtil.LOCAL_PROJECT_DIR.getCanonicalPath();
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
            return filter(localDir, value, getProps());
        }
        static String filter(String localDir, Prop value, Map<String, Map<String, Prop>> props) {
            if ((value == null) || (!value.value.contains("${"))) {
                return (value == null ? null : value.value);
            }
            // the cache key must contain the scope as the same value name may be used in each different scope with
            // different filtered values (i.e., ${project.src.dir} may be the same key in both the default and 'test'
            // scopes but its resolution may be different).
            String cacheKey = localDir + "#" + value.scope + "#" + value.value;
            if (FILTER_CACHE.containsKey(cacheKey)) {
                return FILTER_CACHE.get(cacheKey);
            }
            String filtered = value.value;
            // first attempt to resolve via the value's own context.
            filtering: {
                filtered = filterBy(localDir, filtered, "", props.get(value.context), props);
                if (!value.value.contains("${")) {
                    break filtering;
                }
                // also attempt to filter context-prefixed values
                for (String context : props.keySet()) {
                    filtered = filterBy(localDir, filtered, context + ".", props.get(context), props);
                    if (!value.value.contains("${")) {
                        break filtering;
                    }
                }
                for (String enivronmentProperty : System.getenv().keySet()) {
                    if (filtered.contains("${" + enivronmentProperty + "}")) {
                        try {
                            filtered = filtered.replaceAll(Pattern.quote("${" + enivronmentProperty + "}"),
                                    System.getenv(enivronmentProperty));
                        } catch (IllegalArgumentException iae) {
                            Output.print("^error^ Error filtering '^b^%s^r^' with '^b^%s^r^'.", enivronmentProperty,
                                    System.getenv(enivronmentProperty));
                            Output.print(iae);
                        }
                    }
                    if (!value.value.contains("${")) {
                        break filtering;
                    }
                }
            }
            // don't output large values in their entirety
            String outputFiltered = filtered, outputValue = value.value;
            if (filtered.length() > 99) {
                outputFiltered = filtered.substring(0, 99) + " [truncated]";
            }
            if (value.value.length() > 99) {
                outputValue = value.value.substring(0, 99) + " [truncated]";
            }
            Output.print("^dbug^ filtered ^b^%s^r^ to ^b^%s^r^ [ in %s ].", outputValue, outputFiltered, value.context);
            FILTER_CACHE.put(cacheKey, filtered);
            return filtered;
        }

        private static String filterBy(String localDir, String value, String prefix, Map<String, Prop> props,
                                       Map<String, Map<String, Prop>> all) {
            if (props == null) {
                return value;
            }
            for (String name : props.keySet()) {
                String toFind = prefix + name;
                if (value.contains("${" + toFind + "}")) {
                    String replacement = filter(localDir, props.get(name), all);
                    try {
                        value = value.replaceAll(Pattern.quote("${" + toFind + "}"), replacement);
                    } catch (IllegalArgumentException iae) {
                        Output.print("^error^ Error filtering '^b^%s^r^' with '^b^%s^r^'.", toFind, replacement);
                        Output.print(iae);
                    }
                }
            }
            return value;
        }

        private Singleton() { }

    }

    /**
     * @param propertyName for which to retrieve the {@link Prop}
     * @return the {@link Prop} named {@code propertyName} within the {@link #DEFAULT_CONTEXT} or null if none exists
     */
    public static Prop get(String propertyName) {
        return get(DEFAULT_CONTEXT, propertyName);
    }

    /**
     * @param propertyName for which to retrieve the {@link Prop} object's value.
     * @return the property value for {@code propertyName} within the {@link #DEFAULT_CONTEXT} or the emptry string
     *         if none exists
     */
    public static String getValue(String propertyName) {
        return getValue(DEFAULT_CONTEXT, propertyName);
    }

    /**
     * Retrieves the property associated with {@code propertyName} within {@code context}.
     * @param context from which to look for {@code propertyName}
     * @param propertyName for which to retrieve the {@link Prop}
     * @return the {@link Prop} named {@code propertyName} within context {@code context}
     */
    public static Prop get(String context, String propertyName) {
        return Singleton.get(context, propertyName);
    }

    /**
     * Calls {@link #get(String, String)} and returns the retrieved property's value or an empty string if
     * the no property was found.
     * @param context from which to look for {@code propertyName}
     * @param propertyName for which to retrieve the value
     * @return the property's value or the empty string
     * @see {@link #get(String, String)}
     */
    public static String getValue(String context, String propertyName) {
        return Singleton.getValue(context, propertyName);
    }

    /**
     * @return a mapping from context to its map of properties
     */
    public static Map<String, Map<String, Prop>> getProps() {
        return Singleton.getProps();
    }

    /**
     * @param context from which to retrieve all properties
     * @return a mapping of all property names to properties from within {@code context}, or null if no such context
     *         exists
     */
    public static Map<String, Prop> getProps(String context) {
        return Singleton.getProps(context);
    }

    /**
     * Note, the returned mapping may have one item if {@code propertyNameLike} was directly matched
     * or only one property name matched the wildcard-ed name.
     * @param context from which to retrieve all properties which match {@code propertyNameLike}
     * @param propertyNameLike the property name (which may contain wildcards)
     * @return a mapping of all property names to properties from within {@code context} which match
     *         {@code propertyNameLike} or null if nothing is found.
     */
    public static Map<String, Prop> getProps(String context, String propertyNameLike) {
        return Singleton.getProps(context, propertyNameLike);
    }

    /**
     * Filters {@code prop} by resolving all unix-style properties defined within against the resolved properties
     * of this configuration as well as the system environment variables (to capture things like {@literal PLY_HOME}).
     * @param prop to filter
     * @return the filtered value
     */
    public static String filter(Prop prop) {
        return Singleton.filter(prop);
    }

    static String filterForPly(File projectConfigDir, Prop prop, String scope) {
        String projectConfigCanonicalDir;
        try {
            projectConfigCanonicalDir = projectConfigDir.getCanonicalPath();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        return Singleton.filter(projectConfigCanonicalDir, prop, Loader.loadProjectProps(projectConfigDir, scope));
    }

    static String filterForPly(String localDir, Prop prop, Map<String, Map<String, Prop>> props) {
        return Singleton.filter(localDir, prop, props);
    }

    private Props() { }

}