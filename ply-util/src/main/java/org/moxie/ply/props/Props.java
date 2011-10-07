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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * User: blangel
 * Date: 10/6/11
 * Time: 10:13 AM
 *
 * Assists in resolving properties for given contexts and scopes.
 */
public class Props {

    /**
     * The default context to use if none is specified.
     */
    public static final String DEFAULT_CONTEXT = "ply";

    /**
     * The default scope to use if none is specified.
     */
    public static final String DEFAULT_SCOPE = "";

    /**
     * A {@link java.io.FilenameFilter} for {@link java.util.Properties} files.
     */
    public static final FilenameFilter PROPERTIES_FILENAME_FILTER = new FilenameFilter() {
        @Override public boolean accept(File dir, String name) {
            return name.endsWith(".properties");
        }
    };

    /**
     * Mapping of context -> scope -> property_name -> prop
     */
    private static final Map<String, Map<String, Map<String, Prop>>> PROPS = new HashMap<String, Map<String, Map<String, Prop>>>();

    /**
     * Flag to indicate whether the {@link #PROPS} variable has been initialized.
     */
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    /**
     * A mapping of pre-filtered values to filtered-values.
     */
    private static final Map<String, String> filteredCache = new HashMap<String, String>();

    /**
     * Filters {@code value} by resolving all unix-style properties defined within against the resolved properties
     * of this configuration as well as the system environment variables (to capture things like {@literal PLY_HOME}).
     * @param value to filter
     * @return the filtered value
     */
    public static String filter(Prop value) {
        if ((value == null) || (!value.value.contains("${"))) {
            return (value == null ? null : value.value);
        }
        if (filteredCache.containsKey(value.value)) {
            return filteredCache.get(value.value);
        }
        String filtered = value.value;
        // first attempt to resolve via the value's context and scope.
        Map<String, Map<String, Prop>> all = Props.getPropertiesWithCollapsedScope();
        filtered = filterBy(filtered, "", all.get(value.getContextScope()));
        // also attempt to filter context-prefixed values
        for (String context : all.keySet()) {
            filtered = filterBy(filtered, context + ".", all.get(context));
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
        if (props == null) {
            return value;
        }
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
     * @param named the property name for which to look
     * @return the property for the property named {@code named} within the default context for the default scope
     *         or null if no such property exists.
     */
    public static Prop get(String named) {
        return get(null, null, named);
    }

    /**
     * @param named @see {@link #get(String)}
     * @return the value of the result of {@link #get(String)} or empty string if the result is null
     */
    public static String getValue(String named) {
        Prop prop = get(named);
        return (prop == null ? "" : prop.value);
    }

    /**
     * @param context in which to look for {@code property}; if null the default context is used.
     * @param named the property name for which to look
     * @return the property named {@code named} within context {@code context} for the default scope or null if
     *         no such property exists.
     */
    public static Prop get(String context, String named) {
        return get(context, null, named);
    }

    /**
     * @param context @see {@link #get(String, String)}
     * @param named @see {@link #get(String, String)}
     * @return the value of the result of {@link #get(String, String)} or empty string if the result is null
     */
    public static String getValue(String context, String named) {
        Prop prop = get(context, named);
        return (prop == null ? "" : prop.value);
    }

    /**
     * @param context in which to look for {@code named}; if null the default context is used.
     * @param scope under {@code context} in which to look for {@literal named}; if null the default scope is used.
     * @param named the property name for which to look
     * @return the property named {@code named} within context {@code context} having scope {@code scope}
     *         or null if no such property exists.
     */
    public static Prop get(String context, String scope, String named) {
        if (named == null) {
            return null;
        }
        context = (context == null ? DEFAULT_CONTEXT : context);
        scope = (scope == null ? DEFAULT_SCOPE : scope);

        Map<String, Map<String, Map<String, Prop>>> props = get();
        if (!props.containsKey(context) || !props.get(context).containsKey(scope)) {
            return null;
        }

        // if the given scope (which is not the default) does not contain the prop, get from default scope
        if (!scope.isEmpty() && !props.get(context).get(scope).containsKey(named)) {
            return props.get(context).get("").get(named);
        } else {
            return props.get(context).get(scope).get(named);
        }
    }

    /**
     * @param context @see {@link #get(String, String, String)}
     * @param scope @see {@link #get(String, String, String)}
     * @param named @see {@link #get(String, String, String)}
     * @return the value of the result of {@link #get(String, String, String)} or empty string if the result is null
     */
    public static String getValue(String context, String scope, String named) {
        Prop prop = get(context, scope, named);
        return (prop == null ? "" : prop.value);
    }

    /**
     * @return a resolved mapping of context -> scope -> property_name -> prop
     */
    public static Map<String, Map<String, Map<String, Prop>>> getAllProperties() {
        return Collections.unmodifiableMap(get());
    }

    /**
     * @return a resolved mapping of context (for default scope) -> property-name -> prop
     */
    public static Map<String, Map<String, Prop>> getProperties() {
        Map<String, Map<String, Map<String, Prop>>> props = get();
        Map<String, Map<String, Prop>> defaultScoped = new HashMap<String, Map<String, Prop>>();
        for (String context : props.keySet()) {
            defaultScoped.put(context, props.get(context).get(DEFAULT_SCOPE));
        }
        return defaultScoped;
    }

    /**
     * @param context for which to retrieve scoped properties
     * @return the {@code context}'s resolved mapping of scope -> property_name -> prop or null if there is
     *         no {@code context} mapping
     */
    public static Map<String, Map<String, Prop>> getProperties(String context) {
        return get().get(context);
    }

    /**
     * @param context for which to retrieve properties for {@code scope}
     * @param scope within {@code context} for which to retrieve properties
     * @return a resolved mapping of property_name -> prop for the given {@code context} and {@code scope} or null
     *         if there is no such mapping
     */
    public static Map<String, Prop> getProperties(String context, String scope) {
        Map<String, Map<String, Prop>> contextProps = getProperties(context);
        if (contextProps == null) {
            return null;
        }
        if (DEFAULT_SCOPE.equals(scope)) {
            return contextProps.get(scope);
        }
        // need to include the default scoped props as scopes all inherit from the default scope.
        Map<String ,Prop> defaultScopedProps = contextProps.get(DEFAULT_SCOPE);
        Map<String, Prop> scopedProps = contextProps.get(scope);
        Map<String, Prop> combined = new HashMap<String, Prop>();
        if (defaultScopedProps != null) {
            for (String name : defaultScopedProps.keySet()) {
                combined.put(name, defaultScopedProps.get(name));
            }
        }
        if (scopedProps != null) {
            for (String name : scopedProps.keySet()) {
                combined.put(name, scopedProps.get(name));
            }
        }
        return combined;
    }

    /**
     * Collapses context and scope into one value concatenated with '.' (like they are saved as).
     * @return a resolved mapping of context.scope -> property-name -> prop
     */
    public static Map<String, Map<String, Prop>> getPropertiesWithCollapsedScope() {
        Map<String, Map<String, Map<String, Prop>>> props = get();
        Map<String, Map<String, Prop>> collapsedScope = new HashMap<String, Map<String, Prop>>();
        for (String context : props.keySet()) {
            Map<String, Map<String, Prop>> contextProps = props.get(context);
            for (String scope : contextProps.keySet()) {
                Map<String, Prop> scopedProps = contextProps.get(scope);
                if (scope.isEmpty()) {
                    collapsedScope.put(context, scopedProps);
                } else {
                    Map<String, Prop> augmentedScopedProps = new HashMap<String, Prop>(scopedProps.size());
                    // need to include the default scoped props as scopes all inherit from the default scope.
                    Map<String, Prop> defaultProps = contextProps.get(Props.DEFAULT_SCOPE);
                    if (defaultProps != null) {
                        augmentedScopedProps.putAll(defaultProps);
                    }
                    // add after to override
                    augmentedScopedProps.putAll(scopedProps);
                    collapsedScope.put(context + "." + scope, augmentedScopedProps);
                }
            }
        }
        return collapsedScope;
    }

    /**
     * @param name to match (may include a wildcard) within all contexts.
     * @return a mapping of context.scope to all their properties which match {@code name}
     */
    public static Map<String, Map<String, Prop>> getPropertiesWithCollapsedScope(String name) {
        Map<String, Map<String, Prop>> resolved = new HashMap<String, Map<String, Prop>>();
        Map<String, Map<String, Prop>> all = getPropertiesWithCollapsedScope();
        for (String context : all.keySet()) {
            Map<String, Prop> props = getPropertiesWithCollapsedScope(context, name);
            if (!props.isEmpty()) {
                resolved.put(context, props);
            }
        }
        return resolved;
    }

    /**
     * @param contextDotScope of the properties file to look within
     * @param name to match (may include a wildcard).
     * @return a mapping of all properties within the context.scope which match {@code name}
     */
    public static Map<String, Prop> getPropertiesWithCollapsedScope(String contextDotScope, String name) {
        Map<String, Map<String, Prop>> all = getPropertiesWithCollapsedScope();
        Map<String, Prop> props = all.get(contextDotScope);
        if (props == null) {
            return Collections.emptyMap();
        }

        Map<String, Prop> resolvedProps;
        if (name.contains("*")) {
            resolvedProps = getPropertyValuesByWildcardName(name, props);
        } else {
            resolvedProps = new HashMap<String, Prop>(1);
            if (props.containsKey(name)) {
                resolvedProps.put(name, props.get(name));
            }
        }
        return resolvedProps;
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

    private static Map<String, Map<String, Map<String, Prop>>> get() {
        initProps();
        return PROPS;
    }

    /**
     * Initializes the {@link #PROPS} value exactly once even if called multiple times.  This method is
     * synchronized to ensure that upon return the {@link #PROPS} variable is guaranteed to be in a usable state
     * for the calling thread.
     *
     * If a call to {@link System#getenv(String)} with value "ply.log.levels" returns a non-null value then
     * {@link #PROPS} will be populated by the environment variables; otherwise, it will be populated by
     * resolution via the file-system.
     */
    private static synchronized void initProps() {
        if (INITIALIZED.getAndSet(true)) {
            return;
        }
        // determine if resolution needs to be done via file-system or has been passed via env-properties.
        // TODO - is this the best way? maybe use a more explicit property
        if (System.getenv("ply$ply#log.levels") != null) {
            initPropsByEnv();
        } else {
            initPropsbyFileSystem();
        }
    }

    /**
     * Populates {@link #PROPS} by extracting variables from the environment.  A variable is included
     * in the {@link #PROPS} if its key is prefixed with {@literal ply$}.  It then must conform to the format:
     * context[#scope]#propertyName
     * where scope is optional.  Thus the minimal length of a ply environment key is 7; 4 for the 'ply$' prefix
     * one for the context, one for the period and one for the property name.
     */
    private static void initPropsByEnv() {
        Map<String, String> env = System.getenv();
        for (String key : env.keySet()) {
            if (!key.startsWith("ply$")) {
                continue; // non-ply property
            }
            String propertyValue = env.get(key);
            key = key.substring(4); // strip ply$
            // extract context
            int index = key.indexOf("#"); // error if -1; must have been set by Ply itself
            String context = key.substring(0, index);
            key = key.substring(index + 1); // error if (length == index + 1) as property name's are non-null
            // extract scope
            String scope = "";
            index = key.indexOf("#");
            if (index != -1) {
                scope = key.substring(0, index);
                key = key.substring(index + 1); // error if (length == index + 1) as property name's are non-null
            }
            String propertyName = key;
            set(context, scope, propertyName, propertyValue, null);
        }
    }

    /**
     * @return all properties mapped by 'ply$context[#scope]#propertyName
     */
    public static Map<String, Prop> exportPropsToEnv() {
        Map<String, Map<String, Map<String, Prop>>> props = get();
        Map<String, Prop> env = new HashMap<String, Prop>();
        for (String context : props.keySet()) {
            Map<String, Map<String, Prop>> contextProps = props.get(context);
            for (String scope : contextProps.keySet()) {
                String concat = "ply$" + context + (scope.isEmpty() ? "" : "#" + scope);
                Map<String, Prop> scopeProps = contextProps.get(scope);
                for (String property : scopeProps.keySet()) {
                    env.put(concat + "#" + property, scopeProps.get(property));
                }
            }
        }
        return env;
    }

    private static void initPropsbyFileSystem() {
        // first add the properties from the install directory.
        if (!PlyUtil.SYSTEM_CONFIG_DIR.exists()) {
            Output.print("^error^ the ply install directory is corrupt, please re-install.");
            System.exit(1);
        }
        resolvePropertiesFromDirectory(PlyUtil.SYSTEM_CONFIG_DIR, false);
        // now override with the local project's config directory.
        if (PlyUtil.LOCAL_CONFIG_DIR.exists()) {
            resolvePropertiesFromDirectory(PlyUtil.LOCAL_CONFIG_DIR, true);
        } else {
            throw new IllegalStateException("^warn^ not a ply project (or any of the parent directories), please initialize first ^b^ply init^r^.");
        }
    }

    /**
     * Iterates over the property files within {@code fromDirectory} and calls
     * {@link #resolvePropertiesFromFile(String, String, java.util.Properties, boolean)} on each (provided the file is
     * not a directory).
     * @param fromDirectory the directory from which to resolve properties.
     * @param local true if the {@code fromDirectory} is the local configuration directory
     * @see {@link #PROPERTIES_FILENAME_FILTER}
     */
    private static void resolvePropertiesFromDirectory(File fromDirectory, boolean local) {
        for (File subFile : fromDirectory.listFiles(PROPERTIES_FILENAME_FILTER)) {
            if (!subFile.isDirectory()) {
                String fileName = subFile.getName();
                int index = fileName.lastIndexOf(".properties"); // not == -1 because of PROPERTIES_FILENAME_FILTER
                String context = fileName.substring(0, index);
                // check for scope
                String scope = "";
                index = context.indexOf(".");
                if (index != -1) {
                    scope = context.substring(index + 1);
                    context = context.substring(0, index);
                }
                Properties properties = PropertiesFileUtil.load(subFile.getPath());
                resolvePropertiesFromFile(context, scope, properties, local);
            }
        }
    }

    /**
     * Loads the properties from {@code properties} into the {@link #PROPS} mapping for {@code context}
     * @param context associated with {@code properties}
     * @param scope associated with {@code context}
     * @param properties the loaded properties file
     * @param local true if the {@code properties} is from the local configuration directory
     */
    private static void resolvePropertiesFromFile(String context, String scope, Properties properties, boolean local) {
        for (String propertyName : properties.stringPropertyNames()) {
            set(context, scope, propertyName, properties.getProperty(propertyName), local);
        }
    }

    /**
     * Maps {@code propertyName} to {@code propertyValue} for the given {@code context} and {@code scope}.
     * If either of the backing {@link Map} objects for the given {@code context} and {@code scope} does not exists
     * it will be created.
     * @param context in which to map {@code propertyName} to {@code propertyValue}
     * @param scope of the {@code context} in which to map {@code propertyName} to {@code propertyValue}
     * @param propertyName of the {@code propertyValue} to map
     * @param propertyValue to map
     * @param localOverride true if the property is overriding a system default (or null if unknown because of env resolution).
     */
    private static void set(String context, String scope, String propertyName, String propertyValue, Boolean localOverride) {
        Map<String, Map<String, Prop>> contextProps = PROPS.get(context);
        if (contextProps == null) {
            contextProps = new HashMap<String, Map<String, Prop>>();
            PROPS.put(context, contextProps);
        }
        Map<String, Prop> scopeProps = contextProps.get(scope);
        if (scopeProps == null) {
            scopeProps = new HashMap<String, Prop>();
            contextProps.put(scope, scopeProps);
        }
        scopeProps.put(propertyName, new Prop(context, scope, propertyName, propertyValue, localOverride));
    }

}
