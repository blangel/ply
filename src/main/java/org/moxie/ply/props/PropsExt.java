package org.moxie.ply.props;

import org.moxie.ply.Output;
import org.moxie.ply.PlyUtil;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * User: blangel
 * Date: 10/21/11
 * Time: 12:24 PM
 *
 * Extensions to {@link Props} which should only be visible/applicable for the ply application itself (not any dependent
 * scripts).
 */
public final class PropsExt {

    /**
     * A cache of project-path/scope to resolved environment properties.
     */
    private static final Map<String, Map<String, String>> RESOLVED_ENV_CACHE = new HashMap<String, Map<String, String>>();

    /**
     * If the scope is not the default and the property is not found the default-scope will be consulted
     * @param context to find {@code propertyName}
     * @param scope to find {@code propertyName}
     * @param propertyName of the property to retrieve
     * @return the property for {@code context} and {@code scope} named {@code propertyName} or null if
     *         no such property exists
     */
    public static Prop get(String context, String scope, String propertyName) {
        String contextScope = context + ((scope == null) || scope.isEmpty() ? "" : "." + scope);
        Prop prop = Props.get(contextScope, propertyName);
        if (prop == null) {
            prop = Props.get(context, propertyName);
        }
        return prop;
    }

    /**
     * If the scope is not the default and the property is not found the default-scope will be consulted
     * @param context to find {@code propertyName}
     * @param scope to find {@code propertyName}
     * @param propertyName of the property to retrieve
     * @return the property value for {@code context} and {@code scope} named {@code propertyName} or empty string if
     *         no such property exists
     */
    public static String getValue(String context, String scope, String propertyName) {
        Prop prop = get(context, scope, propertyName);
        return (prop == null ? "" : prop.value);
    }

    /**
     * @param scope of the properties to include in the environment properties mapping
     * @return a mapping of env-property-name to property value (using {@code scope}) for the current project
     * @see {@link #getPropsForEnv(java.io.File, java.io.File, String)}
     * @see {@link PlyUtil#LOCAL_PROJECT_DIR} and {@link PlyUtil#LOCAL_CONFIG_DIR}
     */
    public static Map<String, String> getPropsForEnv(String scope) {
        return getPropsForEnv(PlyUtil.LOCAL_PROJECT_DIR, PlyUtil.LOCAL_CONFIG_DIR, scope);
    }

    /**
     * From the resolved properties, creates a mapping appropriate for exporting to a process's system environment
     * variables.  The mapping returned by this method will only include the contexts' {@code scope} (and the default scope's
     * if the given {@code scope} didn't override the default scope's property).
     * The key is composed of 'ply$' (to distinguish ply variables from other system environment variables) concatenated with
     * the context concatenated with '.' and the property name (note, the scope has been discarded).
     * @param projectDir of the project for which to resolve environment properties.
     * @param projectConfigDir configuration directory associated with {@code projectDir} (typically based at {@code projectDir}
     *                         and named {@literal .ply/config}).
     * @param scope of the properties to include in the environment properties mapping
     * @return a mapping of env-property-name to property value (using {@code scope})
     */
    @SuppressWarnings("unchecked")
    public static Map<String, String> getPropsForEnv(File projectDir, File projectConfigDir, String scope) {
        String cacheKey = projectConfigDir.getPath() + File.separator + scope;
        if (RESOLVED_ENV_CACHE.containsKey(cacheKey)) {
            return RESOLVED_ENV_CACHE.get(cacheKey);
        }
        Map<String, Map<String, Prop>> scopedProps = Loader.loadProjectProps(projectConfigDir, scope);
        Map<String, String> envProps = new HashMap<String, String>(scopedProps.size() * 5); // assume avg of 5 props per context?
        for (String context : scopedProps.keySet()) {
            Map<String, Prop> contextProps = scopedProps.get(context);
            for (String propertyName : contextProps.keySet()) {
                String envKey = "ply$" + context + "." + propertyName;
                Prop prop = contextProps.get(propertyName);
                Prop userScopedProp = new Prop(context, scope, propertyName, prop.value, false);
                envProps.put(envKey, Props.filterForPly(userScopedProp, scopedProps));
            }
        }
        // now add some synthetic properties like the local ply directory location.
        envProps.put("ply$ply.project.dir", projectDir.getPath());
        envProps.put("ply$ply.java", System.getProperty("ply.java"));
        // scripts are always executed from the '../.ply/' directory, allow them to know where the 'ply' invocation
        // actually occurred.
        envProps.put("ply$ply.original.user.dir", System.getProperty("user.dir"));
        // allow scripts access to which scope in which they are being invoked.
        envProps.put("ply$ply.scope", (scope == null ? "" : scope));

        RESOLVED_ENV_CACHE.put(cacheKey, envProps);
        return envProps;
    }

    /**
     * If the scope is the default returns {@link Props#getProps(String)} for the supplied {@code context}, otherwise
     * the default scope is augmented with the properties particular to {@code scope}.
     * @param context for which to get properties
     * @param scope for which to get properties
     * @return the property for {@code context} and {@code scope} or null if
     *         no such properties exists
     */
    public static Map<String, Prop> getPropsForScope(String context, String scope) {
        Map<String, Prop> props = new HashMap<String, Prop>();
        Map<String, Prop> defaultScopedProps = Props.getProps(context);
        if (defaultScopedProps != null) {
            props.putAll(defaultScopedProps);
        }
        if ((scope != null) && !scope.isEmpty()) {
            String contextScope = context + "." + scope;
            Map<String, Prop> scopedProps = Props.getProps(contextScope);
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

    public static String filterForPly(Prop prop, String scope) {
        return Props.filterForPly(prop, scope);
    }

    private PropsExt() { }

}
