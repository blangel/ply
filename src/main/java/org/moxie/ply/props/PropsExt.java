package org.moxie.ply.props;

import org.moxie.ply.Output;
import org.moxie.ply.PlyUtil;

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
     * From the resolved properties, creates a mapping appropriate for exporting to a process's system environment
     * variables.  The mapping returned by this method will only include the contexts' {@code scope} (and the default scope's
     * if the given {@code scope} didn't override the default scope's property).
     * The key is composed of 'ply$' (to distinguish ply variables from other system environment variables) concatenated with
     * the context concatenated with '.' and the property name (note, the scope has been discarded).
     * @param scope of the properties to include in the environment properties mapping
     * @return a mapping of env-property-name to property value (using {@code scope})
     */
    @SuppressWarnings("unchecked")
    public static Map<String, String> getPropsForEnv(String scope) {
        if (Props.Cache.contains(Props.Cache.Type.Env, scope)) {
            return Props.Cache.get(Props.Cache.Type.Env, scope, Map.class);
        }
        Map<String, Map<String, Prop>> scopedProps = getPropsForScope(scope);
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
        envProps.put("ply$ply.project.dir", PlyUtil.LOCAL_PROJECT_DIR.getPath());
        envProps.put("ply$ply.java", System.getProperty("ply.java"));
        // scripts are always executed from the '../.ply/' directory, allow them to know where the 'ply' invocation
        // actually occurred.
        envProps.put("ply$ply.original.user.dir", System.getProperty("user.dir"));
        // allow scripts access to which scope in which they are being invoked.
        envProps.put("ply$ply.scope", (scope == null ? "" : scope));
        
        Props.Cache.put(Props.Cache.Type.Env, scope, envProps);
        return envProps;
    }

    private static Map<String, Map<String, Prop>> getPropsForScope(String scope) {
        Map<String, Map<String, Prop>> rawContexts = new HashMap<String, Map<String, Prop>>();
        Map<String, Map<String, Prop>> all = Props.getProps();
        for (String context : all.keySet()) {
            // remove the scope; can't just skip as there may not be a default scope present (e.g., project has no deps but one 'test' dep).
            if (context.contains(".")) {
                context = context.substring(0, context.indexOf("."));
            }
            rawContexts.put(context, getPropsForScope(context, scope));
        }
        return rawContexts;
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
