package org.moxie.ply.props;

import org.moxie.ply.Output;
import org.moxie.ply.PlyUtil;
import org.moxie.ply.PropertiesFileUtil;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
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

    static final class Cache {

        static enum Type {
            Filter, Env
        }

        static final Map<Object, Object> filterCache = new HashMap<Object, Object>();
        static final Map<Object, Object> envCache = new HashMap<Object, Object>();

        static void put(Type type, Object key, Object value) {
            switch (type) {
                case Filter:
                    filterCache.put(key, value); return;
                case Env:
                    envCache.put(key, value); return;
            }
            throw new AssertionError(String.format("Unknown type %s", type));
        }

        static <T> T get(Type type, Object key, Class<T> valueClass) {
            switch (type) {
                case Filter:
                    return valueClass.cast(filterCache.get(key));
                case Env:
                    return valueClass.cast(envCache.get(key));
            }
            throw new AssertionError(String.format("Unknown type %s", type));
        }

        static boolean contains(Type type, Object key) {
            switch (type) {
                case Filter:
                    return filterCache.containsKey(key);
                case Env:
                    return envCache.containsKey(key);
            }
            throw new AssertionError(String.format("Unknown type %s", type));
        }

    }

    /**
     * The lazily loaded singleton class/object responsible for loading the properties and making the properties
     * accessible to the static methods provided by the {@link Props} class.
     */
    private static final class Singleton {

        static final Map<String, Map<String, Prop>> PROPS = new HashMap<String, Map<String, Prop>>();

        /**
         * A {@link java.io.FilenameFilter} for {@link java.util.Properties} files.
         */
        static final FilenameFilter PROPERTIES_FILENAME_FILTER = new FilenameFilter() {
            @Override public boolean accept(File dir, String name) {
                return name.endsWith(".properties");
            }
        };

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
                // from ply itself there is never a scope as it is resolved and exported as the default
                String propertyName = key;
                set(context, propertyName, propertyValue, /* not applicable */ null);
            }
        }

        static void initPropsbyFileSystem() {
            // first add the properties from the install directory.
            resolvePropertiesFromDirectory(PlyUtil.SYSTEM_CONFIG_DIR, false);
            // now override with the local project's config directory.
            resolvePropertiesFromDirectory(PlyUtil.LOCAL_CONFIG_DIR, true);
        }

        /**
         * Iterates over the property files within {@code fromDirectory} and calls
         * {@link #resolvePropertiesFromFile(String, java.util.Properties, boolean)} on each (provided the file is
         * not a directory).
         * @param fromDirectory the directory from which to resolve properties.
         * @param local true if the {@code fromDirectory} is the local configuration directory
         * @see {@link #PROPERTIES_FILENAME_FILTER}
         */
        static void resolvePropertiesFromDirectory(File fromDirectory, boolean local) {
            File[] subFiles = fromDirectory.listFiles(PROPERTIES_FILENAME_FILTER);
            if (subFiles == null) {
                return;
            }
            for (File subFile : subFiles) {
                if (!subFile.isDirectory()) {
                    String fileName = subFile.getName();
                    int index = fileName.lastIndexOf(".properties"); // not == -1 because of PROPERTIES_FILENAME_FILTER
                    String context = fileName.substring(0, index);
                    Properties properties = PropertiesFileUtil.load(subFile.getPath());
                    resolvePropertiesFromFile(context, properties, local);
                }
            }
        }

        /**
         * Loads the properties from {@code properties} into the {@link #PROPS} mapping for {@code context}
         * @param context associated with {@code properties}
         * @param properties the loaded properties file
         * @param local true if the {@code properties} is from the local configuration directory
         */
        static void resolvePropertiesFromFile(String context, Properties properties, boolean local) {
            for (String propertyName : properties.stringPropertyNames()) {
                set(context, propertyName, properties.getProperty(propertyName), local);
            }
        }

        /**
         * Maps {@code propertyName} to {@code propertyValue} for the given {@code context}.
         * If the backing {@link Map} object for the given {@code context} does not exist
         * it will be created.
         * @param context in which to map {@code propertyName} to {@code propertyValue}
         * @param propertyName of the {@code propertyValue} to map
         * @param propertyValue to map
         * @param localOverride true if the property is overriding a system default (or null if unknown because of env resolution).
         */
        static void set(String context, String propertyName, String propertyValue, Boolean localOverride) {
            Map<String, Prop> contextProps = PROPS.get(context);
            if (contextProps == null) {
                contextProps = new HashMap<String, Prop>();
                PROPS.put(context, contextProps);
            }
            contextProps.put(propertyName, new Prop(context, "" /* TODO - remove scope from Prop */, propertyName, propertyValue, localOverride));
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

        private static Map<String, Map<String, Prop>> getPropsForScope(String scope) {
            Map<String, Map<String, Prop>> rawContexts = new HashMap<String, Map<String, Prop>>();
            for (String context : PROPS.keySet()) {
                // ignore if the context contains a scope
                if (context.contains(".")) {
                    continue;
                }
                rawContexts.put(context, getPropsForScope(context, scope));
            }
            return rawContexts;
        }

        /**
         * If the scope is the default returns {@link #getProps(String)} for the supplied {@code context}, otherwise
         * the default scope is augmented with the properties particular to {@code scope}.
         * @param context for which to get properties
         * @param scope for which to get properties
         * @return the property for {@code context} and {@code scope} or null if
         *         no such properties exists
         */
        private static Map<String, Prop> getPropsForScope(String context, String scope) {
            Map<String, Prop> props = new HashMap<String, Prop>();
            Map<String, Prop> defaultScopedProps = getProps(context);
            if (defaultScopedProps != null) {
                props.putAll(defaultScopedProps);
            }
            if ((scope != null) && !scope.isEmpty()) {
                String contextScope = context + "." + scope;
                Map<String, Prop> scopedProps = getProps(contextScope);
                if (scopedProps != null) {
                    // must strip the scope
                    for (String scopedPropsKey : scopedProps.keySet()) {
                        Prop prop = scopedProps.get(scopedPropsKey);
                        props.put(scopedPropsKey, new Prop(context, "", prop.name, prop.value, prop.localOverride));
                    }
                }
            }
            return props;
        }

        /**
         * Filters {@code value} by resolving all unix-style properties defined within against the resolved properties
         * of this configuration as well as the system environment variables (to capture things like {@literal PLY_HOME}).
         * @param value to filter
         * @return the filtered value
         */
        static String filter(Prop value) {
            return filter(value, getProps());
        }
        static String filter(Prop value, Map<String, Map<String, Prop>> props) {
            if ((value == null) || (!value.value.contains("${"))) {
                return (value == null ? null : value.value);
            }
            // the cache key must contain the scope as the same value name may be used in each different scope with
            // different filtered values (i.e., ${project.src.dir} may be the same key in both the default and 'test'
            // scopes but its resolution may be different).
            if (Cache.contains(Cache.Type.Filter, value.scope + "#" + value.value)) {
                return Cache.get(Cache.Type.Filter, value.scope + "#" + value.value, String.class);
            }
            String filtered = value.value;
            // first attempt to resolve via the value's own context.
            filtered = filterBy(filtered, "", props.get(value.context), props);
            // also attempt to filter context-prefixed values TODO - if nothing left to filter, short-circuit
            for (String context : props.keySet()) {
                filtered = filterBy(filtered, context + ".", props.get(context), props);
            }
            for (String enivronmentProperty : System.getenv().keySet()) {
                if (filtered.contains("${" + enivronmentProperty + "}")) {
                    filtered = filtered.replaceAll(Pattern.quote("${" + enivronmentProperty + "}"),
                            System.getenv(enivronmentProperty));
                }
            }
            Output.print("^dbug^ filtered ^b^%s^r^ to ^b^%s^r^ [ in %s ].", value.value, filtered, value.context);
            Cache.put(Cache.Type.Filter, value.scope + "#" + value.value, filtered);
            return filtered;
        }

        static String filterBy(String value, String prefix, Map<String, Prop> props, Map<String, Map<String, Prop>> all) {
            if (props == null) {
                return value;
            }
            for (String name : props.keySet()) {
                String toFind = prefix + name;
                if (value.contains("${" + toFind + "}")) {
                    value = value.replaceAll(Pattern.quote("${" + toFind + "}"),
                            filter(props.get(name), all));
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

    static String filterForPly(Prop prop, String scope) {
        return Singleton.filter(prop, Singleton.getPropsForScope(scope));
    }

    static String filterForPly(Prop prop, Map<String, Map<String, Prop>> props) {
        return Singleton.filter(prop, props);
    }

    private Props() { }

}