package net.ocheyedan.ply.props;

import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.PlyUtil;
import net.ocheyedan.ply.PropertiesFileUtil;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * User: blangel
 * Date: 10/25/11
 * Time: 6:47 PM
 *
 * Loads property values from a supplied project directory.
 */
public final class Loader {

    /**
     * A {@link java.io.FilenameFilter} for {@link java.util.Properties} files.
     */
    static final FilenameFilter PROPERTIES_FILENAME_FILTER = new FilenameFilter() {
        @Override public boolean accept(File dir, String name) {
            return name.endsWith(".properties");
        }
    };

    /**
     * A cache of resolved properties.  The key is the combination of project directory from which the map was resolved
     * and the particular scope for which it was resolved (if any).
     */
    private static final Map<String, Map<String, Map<String, Prop>>> cache = new HashMap<String, Map<String, Map<String, Prop>>>();

    /**
     * Ad hoc properties set on the command line to override any resolved property from any directory.
     */
    private static Map<String, Map<String, Prop>> adHocProps = null;

    /**
     * @return the resolved property map for the local project.
     * @see {@link PlyUtil#LOCAL_PROJECT_DIR}
     */
    static Map<String, Map<String, Prop>> loadProjectProps() {
        return loadProjectProps(PlyUtil.LOCAL_CONFIG_DIR);
    }

    /**
     * @param projectConfigDir (i.e., {@literal .ply/config} directory) from which to resolve properties.
     * @return the resolved property map for the project located at {@code projectDir}
     */
    static Map<String, Map<String, Prop>> loadProjectProps(File projectConfigDir) {
        if (cache.containsKey(getCanonicalPath(projectConfigDir))) {
            return cache.get(getCanonicalPath(projectConfigDir));
        }

        Map<String, Map<String, Prop>> properties = new HashMap<String, Map<String, Prop>>();
        // first add the properties from the system directory (everyone gets these).
        resolvePropertiesFromDirectory(PlyUtil.SYSTEM_CONFIG_DIR, false, properties);
        // now look for parental properties
        // TODO - how to do this? force parent to be local (then fairly easy) or can it be by dep-atom ref., if so
        // TODO - a bit harder as we'll need to export properties to the jar and pull from there
        // next, override with the project's config directory.
        resolvePropertiesFromDirectory(projectConfigDir, true, properties);
        // finally, override all with the ad-hoc properties (from the command-line), if any.
        if (adHocProps != null) {
            overrideMerge(properties, adHocProps);
        }

        cache.put(getCanonicalPath(projectConfigDir), properties);

        return properties;
    }

    /**
     * Note, if {@code scope} is null or empty (the default scope) this method returns different results than
     * calling {@link #loadProjectProps()} as this method will remove non-default scoped properties whereas
     * the latter method will maintain them (keyed as context.scope).
     * @param scope of the properties to load (including defaults that the scope doesn't explicitly override).
     * @return the resolved property map of properties scoped as {@code scope} for the current project.
     */
    static Map<String, Map<String, Prop>> loadProjectProps(String scope) {
        return loadProjectProps(PlyUtil.LOCAL_CONFIG_DIR, scope);
    }

    /**
     * The returned map will contain all the default scoped properties overridden by the {@code scope} property, if any.
     * Note, if {@code scope} is null or empty (the default scope) this method returns different results than
     * calling {@link #loadProjectProps(File)} as this method will remove non-default scoped properties whereas
     * the latter method will maintain them (keyed as context.scope).
     * @param projectConfigDir (i.e., {@literal .ply/config} directory) from which to resolve properties.
     * @param scope of the properties to load (including defaults that the scope doesn't explicitly override).
     * @return the resolved property map of properties scoped as {@code scope} for the project located at {@code projectDir}
     */
    static Map<String, Map<String, Prop>> loadProjectProps(File projectConfigDir, String scope) {
        // normalize default scope to be the empty string
        scope = ((scope == null) || scope.isEmpty() ? "" : scope);
        // ensure the cache-key is different than that made by {@link #loadProjectProps(File)} as on default scope
        // this method returns only the default scoped properties, no other scope.
        String projectConfigCanonicalDirPath = getCanonicalPath(projectConfigDir);
        String cacheKey = projectConfigCanonicalDirPath + File.separator + "#" + scope;
        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }
        Map<String, Map<String, Prop>> all = loadProjectProps(projectConfigDir);
        // override all with the ad-hoc properties (from the command-line), if any.
        if (adHocProps != null) {
            overrideMerge(all, adHocProps);
        }
        Map<String, Map<String, Prop>> scoped = new HashMap<String, Map<String, Prop>>(all.size());

        for (String scopedContext : all.keySet()) {
            // separate the context and scope
            String curScope = "", context = scopedContext;
            if (scopedContext.contains(".")) {
                int dotIndex = scopedContext.indexOf(".");
                context = scopedContext.substring(0, dotIndex);
                curScope = scopedContext.substring(dotIndex + 1, scopedContext.length());
            }
            // if this is a non-default scope but not our scope, disregard
            if (!curScope.isEmpty() && !curScope.equals(scope)) {
                continue;
            }
            // if this is the default scope and the curScope is not the default, disregard
            else if (scope.isEmpty() && !curScope.isEmpty()) {
                continue;
            }
            // get/make the context's prop map
            Map<String, Prop> props = all.get(scopedContext);
            Map<String, Prop> scopedProps = scoped.get(context);
            if (scopedProps == null) {
                scopedProps = new HashMap<String, Prop>(props.size());
                scoped.put(context, scopedProps);
            }
            // add properties to {@code scopedProps} from {@code props}
            for (String key : props.keySet()) {
                Prop prop = props.get(key);
                // default scope case (only add if not already in the {@code scopedProps}
                if (curScope.isEmpty() && !scopedProps.containsKey(key)) {
                    scopedProps.put(key, new Prop(context, "", prop.name, prop.value, prop.localOverride));
                }
                // if non-default scope case, add no matter what (as there's only the default scope or non-default scope,
                // and non-default overrides default).
                else if (!curScope.isEmpty()) {
                    scopedProps.put(key, new Prop(context, scope, prop.name, prop.value, prop.localOverride));
                }
            }
        }

        cache.put(cacheKey, scoped);
        return scoped;
    }

    /**
     * @param adHocProps to set to {@link Loader#adHocProps}
     */
    static void setAdHocProps(Map<String, Map<String, Prop>> adHocProps) {
        if (Loader.adHocProps == null) {
            Loader.adHocProps = new HashMap<String, Map<String, Prop>>();
        }
        for (String key : adHocProps.keySet()) {
            Map<String, Prop> props = Loader.adHocProps.get(key);
            if (props == null) {
                props = new HashMap<String, Prop>();
                Loader.adHocProps.put(key, props);
            }
            Map<String, Prop> adHocPropsMap = adHocProps.get(key);
            for (String propKey : adHocPropsMap.keySet()) {
                props.put(propKey, adHocPropsMap.get(propKey));
            }
        }
        // TODO - better way to ensure all ad-hoc prop state is re-interpretted by Output.
        for (String key : adHocProps.keySet()) {
            Map<String, Prop> adHocPropsByKey = adHocProps.get(key);
            for (String propName : adHocPropsByKey.keySet()) {
                Prop adHocProp = adHocPropsByKey.get(propName);
                Output.handleAdHocProp(adHocProp);
            }
        }
    }

    /**
     * Iterates over the property files within {@code fromDirectory} and calls
     * {@link #resolvePropertiesFromFile(String, java.util.Properties, boolean, Map)} on each (provided the file is
     * not a directory).
     * @param fromDirectory the directory from which to resolve properties.
     * @param local true if the {@code fromDirectory} is the local configuration directory.
     * @param placement map into which resolved properties will be placed
     * @see {@link #PROPERTIES_FILENAME_FILTER}
     */
    static void resolvePropertiesFromDirectory(File fromDirectory, boolean local,
                                               Map<String, Map<String, Prop>> placement) {
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
                resolvePropertiesFromFile(context, properties, local, placement);
            }
        }
    }

    /**
     * Loads the properties from {@code properties} into the {@code placement} mapping for {@code context}
     * @param context associated with {@code properties}
     * @param properties the loaded properties file
     * @param local true if the {@code properties} is from the local configuration directory
     * @param placement map into which resolved properties will be placed
     */
    private static void resolvePropertiesFromFile(String context, Properties properties, boolean local,
                                                  Map<String, Map<String, Prop>> placement) {
        for (String propertyName : properties.stringPropertyNames()) {
            Map<String, Prop> contextProps = placement.get(context);
            if (contextProps == null) {
                contextProps = new HashMap<String, Prop>();
                placement.put(context, contextProps);
            }
            String propertyValue = properties.getProperty(propertyName);
            contextProps.put(propertyName, new Prop(context, "", propertyName, propertyValue, local));
        }
    }

    private static String getCanonicalPath(File dir) {
        try {
            return dir.getCanonicalPath();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    private static void overrideMerge(Map<String, Map<String, Prop>> toOverride, Map<String, Map<String, Prop>> overrider) {
        for (String context : overrider.keySet()) {
            if (!toOverride.containsKey(context)) {
                toOverride.put(context, overrider.get(context));
            } else {
                Map<String, Prop> contextPropsToOverride = toOverride.get(context);
                Map<String, Prop> contextPropsOverrider = overrider.get(context);
                for (String propName : contextPropsOverrider.keySet()) {
                    contextPropsToOverride.put(propName, contextPropsOverrider.get(propName));
                }
            }
        }
    }

    private Loader() { }

}
