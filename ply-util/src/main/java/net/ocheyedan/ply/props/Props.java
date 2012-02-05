package net.ocheyedan.ply.props;

import net.ocheyedan.ply.PlyUtil;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * User: blangel
 * Date: 10/6/11
 * Time: 10:13 AM
 *
 * Provides access to the properties configured by the user for a given project directory.
 */
public class Props {

    /**
     * @return all properties (those within {@link PlyUtil#LOCAL_CONFIG_DIR} and {@link PlyUtil#SYSTEM_CONFIG_DIR}) for
     *         all contexts with the current scope
     * @see #getScope
     */
    public static Map<Context, Collection<Prop>> get() {
        return get(getScope());
    }

    /**
     * As opposed to {@link #get()}, this will force the properties
     * to be resolved from the file system and not from environment variables, if any.
     * @return all properties (those within {@link PlyUtil#LOCAL_CONFIG_DIR} and {@link PlyUtil#SYSTEM_CONFIG_DIR}) for
     *         all contexts with the current scope
     * @see #getScope
     */
    public static Map<Context, Collection<Prop>> getForceResolution() {
        return getForceResolution(getScope());
    }

    /**
     * @return all properties defined within {@link PlyUtil#LOCAL_CONFIG_DIR} for all contexts with the current scope
     * @see #getScope
     */
    public static Map<Context, Collection<Prop>> getLocal() {
        return getLocal(getScope());
    }
    
    /**
     * As opposed to {@link #getLocal()}, this will force the properties
     * to be resolved from the file system and not from environment variables, if any.
     * @return all properties defined within {@link PlyUtil#LOCAL_CONFIG_DIR} for all contexts with the current scope
     * @see #getScope
     */
    public static Map<Context, Collection<Prop>> getLocalForceResolution() {
        return getLocalForceResolution(getScope());
    }

    /**
     * @param configDirectory a ply-config directory from which to retrieve properties (i.e., {@literal .ply/config}).
     * @return all properties defined within {@code configDirectory} and {@link PlyUtil#SYSTEM_CONFIG_DIR} for all
     *         contexts with the current scope
     * @see #getScope
     */
    public static Map<Context, Collection<Prop>> get(File configDirectory) {
        return get(configDirectory, getScope());
    }

    /**
     * As opposed to {@link #get(File)}, this will force the properties
     * to be resolved from the file system and not from environment variables, if any.
     * @param configDirectory a ply-config directory from which to retrieve properties (i.e., {@literal .ply/config}).
     * @return all properties defined within {@code configDirectory} and {@link PlyUtil#SYSTEM_CONFIG_DIR} for all
     *         contexts with the current scope
     * @see #getScope
     */
    public static Map<Context, Collection<Prop>> getForceResolution(File configDirectory) {
        return getForceResolution(configDirectory, getScope());
    }

    /**
     * @param configDirectory a ply-config directory from which to retrieve properties (i.e., {@literal .ply/config}).
     * @return all properties defined within {@code configDirectory} for all contexts with the current scope
     * @see #getScope
     */
    public static Map<Context, Collection<Prop>> getLocal(File configDirectory) {
        return getLocal(configDirectory, getScope());
    }

    /**
     * As opposed to {@link #getLocal(File)}, this will force the properties
     * to be resolved from the file system and not from environment variables, if any.
     * @param configDirectory a ply-config directory from which to retrieve properties (i.e., {@literal .ply/config}).
     * @return all properties defined within {@code configDirectory} for all contexts with the current scope
     * @see #getScope
     */
    public static Map<Context, Collection<Prop>> getLocalForceResolution(File configDirectory) {
        return getLocalForceResolution(configDirectory, getScope());
    }

    /**
     * @param scope of the properties to retrieve
     * @return all properties (those within {@link PlyUtil#LOCAL_CONFIG_DIR} and {@link PlyUtil#SYSTEM_CONFIG_DIR}) for
     *         all contexts with the given {@code scope}
     */
    public static Map<Context, Collection<Prop>> get(Scope scope) {
        return get(PlyUtil.LOCAL_CONFIG_DIR, scope);
    }

    /**
     * As opposed to {@link #get(Scope)}, this will force the properties
     * to be resolved from the file system and not from environment variables, if any.
     * @param scope of the properties to retrieve
     * @return all properties (those within {@link PlyUtil#LOCAL_CONFIG_DIR} and {@link PlyUtil#SYSTEM_CONFIG_DIR}) for
     *         all contexts with the given {@code scope}
     */
    public static Map<Context, Collection<Prop>> getForceResolution(Scope scope) {
        return getForceResolution(PlyUtil.LOCAL_CONFIG_DIR, scope);
    }

    /**
     * @param scope of the properties to retrieve
     * @return all properties defined within {@link PlyUtil#LOCAL_CONFIG_DIR} for all contexts with the given {@code scope}
     */
    public static Map<Context, Collection<Prop>> getLocal(Scope scope) {
        return getLocal(PlyUtil.LOCAL_CONFIG_DIR, scope);
    }

    /**
     * As opposed to {@link #getLocal(Scope)}, this will force the properties
     * to be resolved from the file system and not from environment variables, if any.
     * @param scope of the properties to retrieve
     * @return all properties defined within {@link PlyUtil#LOCAL_CONFIG_DIR} for all contexts with the given {@code scope}
     */
    public static Map<Context, Collection<Prop>> getLocalForceResolution(Scope scope) {
        return getLocalForceResolution(PlyUtil.LOCAL_CONFIG_DIR, scope);
    }

    /**
     * @param configDirectory a ply-config directory from which to retrieve properties (i.e., {@literal .ply/config}).
     * @param scope of the properties to retrieve
     * @return all properties defined within {@code configDirectory} and {@link PlyUtil#SYSTEM_CONFIG_DIR} for all
     *         contexts with the given {@code scope}
     */
    public static Map<Context, Collection<Prop>> get(File configDirectory, Scope scope) {
        return get(configDirectory, scope, false, false);
    }

    /**
     * As opposed to {@link #get(File, Scope)}, this will force the properties
     * to be resolved from the file system and not from environment variables, if any.
     * @param configDirectory a ply-config directory from which to retrieve properties (i.e., {@literal .ply/config}).
     * @param scope of the properties to retrieve
     * @return all properties defined within {@code configDirectory} and {@link PlyUtil#SYSTEM_CONFIG_DIR} for all
     *         contexts with the given {@code scope}
     */
    public static Map<Context, Collection<Prop>> getForceResolution(File configDirectory, Scope scope) {
        return get(configDirectory, scope, false, true);
    }

    /**
     * @param configDirectory a ply-config directory from which to retrieve properties (i.e., {@literal .ply/config}).
     * @param scope of the properties to retrieve
     * @return  all properties defined within {@code configDirectory} for all contexts with the given {@code scope}
     */
    public static Map<Context, Collection<Prop>> getLocal(File configDirectory, Scope scope) {
        return get(configDirectory, scope, true, false);
    }

    /**
     * As opposed to {@link #getLocal(File, Scope)}, this will force the properties
     * to be resolved from the file system and not from environment variables, if any.
     * @param configDirectory a ply-config directory from which to retrieve properties (i.e., {@literal .ply/config}).
     * @param scope of the properties to retrieve
     * @return  all properties defined within {@code configDirectory} for all contexts with the given {@code scope}
     */
    public static Map<Context, Collection<Prop>> getLocalForceResolution(File configDirectory, Scope scope) {
        return get(configDirectory, scope, true, true);
    }

    private static Map<Context, Collection<Prop>> get(File configDirectory, Scope scope, boolean excludeSystem,
                                                      boolean forceResolution) {
        if (Cache.containsContextMap(configDirectory, scope, excludeSystem, forceResolution)) {
            return Cache.getContextMap(configDirectory, scope, excludeSystem, forceResolution);
        }
        Collection<Prop.All> props = Loader.get(configDirectory, forceResolution);
        Map<Context, Collection<Prop>> map = new HashMap<Context, Collection<Prop>>();
        for (Prop.All prop : props) {
            Prop.Val value = prop.get(scope, excludeSystem);
            if (value == null) {
                continue;
            }
            Collection<Prop> properties = map.get(prop.context);
            if (properties == null) {
                properties = new HashSet<Prop>();
                map.put(prop.context, properties);
            }
            properties.add(new Prop(prop.context, prop.name, value.value, value.unfiltered, value.from));
        }
        Cache.putContextMap(configDirectory, scope, excludeSystem, forceResolution, map);
        return map;
    }

    /**
     * @param context of the properties to retrieve
     * @return all properties (those within {@link PlyUtil#LOCAL_CONFIG_DIR} and {@link PlyUtil#SYSTEM_CONFIG_DIR}) for
     *         the given {@code context} with the current scope
     * @see #getScope
     */
    public static Collection<Prop> get(Context context) {
        return get(context, PlyUtil.LOCAL_CONFIG_DIR);
    }

    /**
     * As opposed to {@link #get(Context)}, this will force the properties
     * to be resolved from the file system and not from environment variables, if any.
     * @param context of the properties to retrieve
     * @return all properties (those within {@link PlyUtil#LOCAL_CONFIG_DIR} and {@link PlyUtil#SYSTEM_CONFIG_DIR}) for
     *         the given {@code context} with the current scope
     * @see #getScope
     */
    public static Collection<Prop> getForceResolution(Context context) {
        return getForceResolution(context, PlyUtil.LOCAL_CONFIG_DIR);
    }

    /**
     * @param context of the properties to retrieve
     * @return all properties defined within {@link PlyUtil#LOCAL_CONFIG_DIR} for the given {@code context} with the
     *         current scope
     * @see #getScope
     */
    public static Collection<Prop> getLocal(Context context) {
        return getLocal(context, PlyUtil.LOCAL_CONFIG_DIR);
    }

    /**
     * As opposed to {@link #getLocal(Context)}, this will force the properties
     * to be resolved from the file system and not from environment variables, if any.
     * @param context of the properties to retrieve
     * @return all properties defined within {@link PlyUtil#LOCAL_CONFIG_DIR} for the given {@code context} with the
     *         current scope
     * @see #getScope
     */
    public static Collection<Prop> getLocalForceResolution(Context context) {
        return getLocalForceResolution(context, PlyUtil.LOCAL_CONFIG_DIR);
    }

    /**
     * @param context of the properties to retrieve
     * @param configDirectory a ply-config directory from which to retrieve properties (i.e., {@literal .ply/config}).
     * @return all properties defined within {@code configDirectory} and {@link PlyUtil#SYSTEM_CONFIG_DIR} for the
     *         given {@code context} and the current scope
     * @see #getScope
     */
    public static Collection<Prop> get(Context context, File configDirectory) {
        return get(context, configDirectory, getScope());
    }

    /**
     * As opposed to {@link #get(Context, File)}, this will force the properties
     * to be resolved from the file system and not from environment variables, if any.
     * @param context of the properties to retrieve
     * @param configDirectory a ply-config directory from which to retrieve properties (i.e., {@literal .ply/config}).
     * @return all properties defined within {@code configDirectory} and {@link PlyUtil#SYSTEM_CONFIG_DIR} for the
     *         given {@code context} and the current scope
     * @see #getScope
     */
    public static Collection<Prop> getForceResolution(Context context, File configDirectory) {
        return getForceResolution(context, configDirectory, getScope());
    }

    /**
     * @param context of the properties to retrieve
     * @param configDirectory a ply-config directory from which to retrieve properties (i.e., {@literal .ply/config}).
     * @return all properties defined within {@code configDirectory} for the given {@code context} and the current scope.
     * @see #getScope
     */
    public static Collection<Prop> getLocal(Context context, File configDirectory) {
        return getLocal(context, configDirectory, getScope());
    }

    /**
     * As opposed to {@link #getLocal(Context, File)}, this will force the properties
     * to be resolved from the file system and not from environment variables, if any.
     * @param context of the properties to retrieve
     * @param configDirectory a ply-config directory from which to retrieve properties (i.e., {@literal .ply/config}).
     * @return all properties defined within {@code configDirectory} for the given {@code context} and the current scope.
     * @see #getScope
     */
    public static Collection<Prop> getLocalForceResolution(Context context, File configDirectory) {
        return getLocalForceResolution(context, configDirectory, getScope());
    }

    /**
     * @param context of the properties to retrieve
     * @param scope of the properties to retrieve
     * @return all properties defined within {@link PlyUtil#LOCAL_CONFIG_DIR} and {@link PlyUtil#SYSTEM_CONFIG_DIR} for
     *         the given {@code context} and the given {@code scope}.
     */
    public static Collection<Prop> get(Context context, Scope scope) {
        return get(context, PlyUtil.LOCAL_CONFIG_DIR, scope);
    }

    /**
     * As opposed to {@link #get(Context, Scope)}, this will force the properties
     * to be resolved from the file system and not from environment variables, if any.
     * @param context of the properties to retrieve
     * @param scope of the properties to retrieve
     * @return all properties defined within {@link PlyUtil#LOCAL_CONFIG_DIR} and {@link PlyUtil#SYSTEM_CONFIG_DIR} for
     *         the given {@code context} and the given {@code scope}.
     */
    public static Collection<Prop> getForceResolution(Context context, Scope scope) {
        return getForceResolution(context, PlyUtil.LOCAL_CONFIG_DIR, scope);
    }

    /**
     * @param context of the properties to retrieve
     * @param scope of the properties to retrieve
     * @return all properties defined within {@link PlyUtil#LOCAL_CONFIG_DIR} for the given {@code context} and the
     *         given {@code scope}.
     */
    public static Collection<Prop> getLocal(Context context, Scope scope) {
        return getLocal(context, PlyUtil.LOCAL_CONFIG_DIR, scope);
    }

    /**
     * As opposed to {@link #getLocal(Context, Scope)}, this will force the properties
     * to be resolved from the file system and not from environment variables, if any.
     * @param context of the properties to retrieve
     * @param scope of the properties to retrieve
     * @return all properties defined within {@link PlyUtil#LOCAL_CONFIG_DIR} for the given {@code context} and the
     *         given {@code scope}.
     */
    public static Collection<Prop> getLocalForceResolution(Context context, Scope scope) {
        return getLocalForceResolution(context, PlyUtil.LOCAL_CONFIG_DIR, scope);
    }

    /**
     * @param context of the properties to retrieve
     * @param configDirectory a ply-config directory from which to retrieve properties (i.e., {@literal .ply/config}).
     * @param scope of the properties to retrieve
     * @return all properties defined within {@code configDirectory} and {@link PlyUtil#SYSTEM_CONFIG_DIR} for the
     *         given {@code context} and the given {@code scope}.
     */
    public static Collection<Prop> get(Context context, File configDirectory, Scope scope) {
        return get(context, configDirectory, scope, false, false);
    }

    /**
     * As opposed to {@link #get(Context, File, Scope)}, this will force the properties
     * to be resolved from the file system and not from environment variables, if any.
     * @param context of the properties to retrieve
     * @param configDirectory a ply-config directory from which to retrieve properties (i.e., {@literal .ply/config}).
     * @param scope of the properties to retrieve
     * @return all properties defined within {@code configDirectory} and {@link PlyUtil#SYSTEM_CONFIG_DIR} for the
     *         given {@code context} and the given {@code scope}.
     */
    public static Collection<Prop> getForceResolution(Context context, File configDirectory, Scope scope) {
        return get(context, configDirectory, scope, false, true);
    }

    /**
     * @param context of the properties to retrieve
     * @param configDirectory a ply-config directory from which to retrieve properties (i.e., {@literal .ply/config}).
     * @param scope of the properties to retrieve
     * @return all properties defined within {@code configDirectory} for the given {@code context} and the
     *         given {@code scope}.
     */
    public static Collection<Prop> getLocal(Context context, File configDirectory, Scope scope) {
        return get(context, configDirectory, scope, true, false);
    }

    /**
     * As opposed to {@link #getLocal(Context, File, Scope)}, this will force the properties
     * to be resolved from the file system and not from environment variables, if any.
     * @param context of the properties to retrieve
     * @param configDirectory a ply-config directory from which to retrieve properties (i.e., {@literal .ply/config}).
     * @param scope of the properties to retrieve
     * @return all properties defined within {@code configDirectory} for the given {@code context} and the
     *         given {@code scope}.
     */
    public static Collection<Prop> getLocalForceResolution(Context context, File configDirectory, Scope scope) {
        return get(context, configDirectory, scope, true, true);
    }

    private static Collection<Prop> get(Context context, File configDirectory, Scope scope, boolean excludeSystem,
                                        boolean forceResolution) {
        if (Cache.containsContext(context, configDirectory, scope, excludeSystem, forceResolution)) {
            return Cache.getContext(context, configDirectory, scope, excludeSystem, forceResolution);
        }
        Collection<Prop.All> props = Loader.get(configDirectory, forceResolution);
        Collection<Prop> properties = new HashSet<Prop>();
        for (Prop.All prop : props) {
            if (!prop.context.equals(context)) {
                continue;
            }
            Prop.Val value = prop.get(scope, excludeSystem);
            if (value == null) {
                continue;
            }
            properties.add(new Prop(prop.context, prop.name, value.value, value.unfiltered, value.from));
        }
        Cache.putContext(context, configDirectory, scope, excludeSystem, forceResolution, properties);
        return properties;
    }

    /**
     * @param context of the property to retrieve
     * @param named the property name to retrieve
     * @return the property named {@code named} (defined within {@link PlyUtil#LOCAL_CONFIG_DIR} or
     *         {@link PlyUtil#SYSTEM_CONFIG_DIR}) for the given {@code context} with the current scope or null
     *         if no such property exists
     * @see #getScope
     */
    public static Prop get(Context context, String named) {
        return get(context, named, PlyUtil.LOCAL_CONFIG_DIR);
    }

    /**
     * As opposed to {@link #get(Context, String)}, this will force the properties
     * to be resolved from the file system and not from environment variables, if any.
     * @param context of the property to retrieve
     * @param named the property name to retrieve
     * @return the property named {@code named} (defined within {@link PlyUtil#LOCAL_CONFIG_DIR} or
     *         {@link PlyUtil#SYSTEM_CONFIG_DIR}) for the given {@code context} with the current scope or null
     *         if no such property exists
     * @see #getScope
     */
    public static Prop getForceResolution(Context context, String named) {
        return getForceResolution(context, named, PlyUtil.LOCAL_CONFIG_DIR);
    }

    /**
     * @param context of the property to retrieve
     * @param named the property name to retrieve
     * @return the property named {@code named} defined within {@link PlyUtil#LOCAL_CONFIG_DIR} for the given
     *         {@code context} with the current scope or null if no such property exists.
     * @see #getScope
     */
    public static Prop getLocal(Context context, String named) {
        return getLocal(context, named, PlyUtil.LOCAL_CONFIG_DIR);
    }

    /**
     * As opposed to {@link #getLocal(Context, String)}, this will force the properties
     * to be resolved from the file system and not from environment variables, if any.
     * @param context of the property to retrieve
     * @param named the property name to retrieve
     * @return the property named {@code named} defined within {@link PlyUtil#LOCAL_CONFIG_DIR} for the given
     *         {@code context} with the current scope or null if no such property exists.
     * @see #getScope
     */
    public static Prop getLocalForceResolution(Context context, String named) {
        return getLocalForceResolution(context, named, PlyUtil.LOCAL_CONFIG_DIR);
    }

    /**
     * @param context of the property to retrieve
     * @param named the property name to retrieve
     * @param configDirectory a ply-config directory from which to retrieve the property (i.e., {@literal .ply/config}).
     * @return the property named {@code named} defined within {@code configDirectory} or {@link PlyUtil#SYSTEM_CONFIG_DIR}
     *         for the given {@code context} and the current scope or null if no such property exists
     * @see #getScope
     */
    public static Prop get(Context context, String named, File configDirectory) {
        return get(context, named, configDirectory, getScope());
    }

    /**
     * As opposed to {@link #get(Context, String, File)}, this will force the properties
     * to be resolved from the file system and not from environment variables, if any.
     * @param context of the property to retrieve
     * @param named the property name to retrieve
     * @param configDirectory a ply-config directory from which to retrieve the property (i.e., {@literal .ply/config}).
     * @return the property named {@code named} defined within {@code configDirectory} or {@link PlyUtil#SYSTEM_CONFIG_DIR}
     *         for the given {@code context} and the current scope or null if no such property exists
     * @see #getScope
     */
    public static Prop getForceResolution(Context context, String named, File configDirectory) {
        return getForceResolution(context, named, configDirectory, getScope());
    }

    /**
     * @param context of the property to retrieve
     * @param named the property name to retrieve
     * @param configDirectory a ply-config directory from which to retrieve the property (i.e., {@literal .ply/config}).
     * @return the property named {@code named} defined within {@code configDirectory} for the given {@code context}
     *         and the current scope or null if no such property exists
     * @see #getScope
     */
    public static Prop getLocal(Context context, String named, File configDirectory) {
        return getLocal(context, named, configDirectory, getScope());
    }

    /**
     * As opposed to {@link #getLocal(Context, String, File)}, this will force the properties
     * to be resolved from the file system and not from environment variables, if any.
     * @param context of the property to retrieve
     * @param named the property name to retrieve
     * @param configDirectory a ply-config directory from which to retrieve the property (i.e., {@literal .ply/config}).
     * @return the property named {@code named} defined within {@code configDirectory} for the given {@code context}
     *         and the current scope or null if no such property exists
     * @see #getScope
     */
    public static Prop getLocalForceResolution(Context context, String named, File configDirectory) {
        return getLocalForceResolution(context, named, configDirectory, getScope());
    }

    /**
     * @param context of the property to retrieve
     * @param named the property name to retrieve
     * @param scope of the property to retrieve
     * @return the property named {@code named} defined within {@link PlyUtil#LOCAL_CONFIG_DIR} or
     *         {@link PlyUtil#SYSTEM_CONFIG_DIR} for the given {@code context} and the given {@code scope} or null if
     *         no such property exists
     */
    public static Prop get(Context context, String named, Scope scope) {
        return get(context, named, PlyUtil.LOCAL_CONFIG_DIR, scope);
    }

    /**
     * As opposed to {@link #get(Context, String, File, Scope)}, this will force the properties
     * to be resolved from the file system and not from environment variables, if any.
     * @param context of the property to retrieve
     * @param named the property name to retrieve
     * @param scope of the property to retrieve
     * @return the property named {@code named} defined within {@link PlyUtil#LOCAL_CONFIG_DIR} or
     *         {@link PlyUtil#SYSTEM_CONFIG_DIR} for the given {@code context} and the given {@code scope} or null if
     *         no such property exists
     */
    public static Prop getForceResolution(Context context, String named, Scope scope) {
        return getForceResolution(context, named, PlyUtil.LOCAL_CONFIG_DIR, scope);
    }

    /**
     * @param context of the property to retrieve
     * @param named the property name to retrieve
     * @param scope of the property to retrieve
     * @return the property named {@code named} defined within {@link PlyUtil#LOCAL_CONFIG_DIR} for the given
     *         {@code context} and the given {@code scope} or null if no such property exists
     */
    public static Prop getLocal(Context context, String named, Scope scope) {
        return getLocal(context, named, PlyUtil.LOCAL_CONFIG_DIR, scope);
    }

    /**
     * As opposed to {@link #getLocal(Context, String, Scope)}, this will force the properties
     * to be resolved from the file system and not from environment variables, if any.
     * @param context of the property to retrieve
     * @param named the property name to retrieve
     * @param scope of the property to retrieve
     * @return the property named {@code named} defined within {@link PlyUtil#LOCAL_CONFIG_DIR} for the given
     *         {@code context} and the given {@code scope} or null if no such property exists
     */
    public static Prop getLocalForceResolution(Context context, String named, Scope scope) {
        return getLocalForceResolution(context, named, PlyUtil.LOCAL_CONFIG_DIR, scope);
    }

    /**
     * @param context of the property to retrieve
     * @param named the property name to retrieve
     * @param configDirectory a ply-config directory from which to retrieve the property (i.e., {@literal .ply/config}).
     * @param scope of the property to retrieve
     * @return the property named {@code named} defined within {@code configDirectory} or
     *         {@link PlyUtil#SYSTEM_CONFIG_DIR} for the given {@code context} and the given {@code scope} or null if no
     *         property exists.
     */
    public static Prop get(Context context, String named, File configDirectory, Scope scope) {
        return get(context, named, configDirectory, scope, false, false);
    }

    /**
     * As opposed to {@link #get(Context, String, File, Scope)}, this will force the properties
     * to be resolved from the file system and not from environment variables, if any.
     * @param context of the property to retrieve
     * @param named the property name to retrieve
     * @param configDirectory a ply-config directory from which to retrieve the property (i.e., {@literal .ply/config}).
     * @param scope of the property to retrieve
     * @return the property named {@code named} defined within {@code configDirectory} or
     *         {@link PlyUtil#SYSTEM_CONFIG_DIR} for the given {@code context} and the given {@code scope} or null if no
     *         property exists.
     */
    public static Prop getForceResolution(Context context, String named, File configDirectory, Scope scope) {
        return get(context, named, configDirectory, scope, false, true);
    }

    /**
     * @param context of the property to retrieve
     * @param named the property name to retrieve
     * @param configDirectory a ply-config directory from which to retrieve the property (i.e., {@literal .ply/config}).
     * @param scope of the property to retrieve
     * @return the property named {@code named} defined within {@code configDirectory} for the given {@code context}
     *         and the given {@code scope} or null if no property exists.
     */
    public static Prop getLocal(Context context, String named, File configDirectory, Scope scope) {
        return get(context, named, configDirectory, scope, true, false);
    }

    /**
     * As opposed to {@link #getLocal(Context, String, File, Scope)}, this will force the properties
     * to be resolved from the file system and not from environment variables, if any.
     * @param context of the property to retrieve
     * @param named the property name to retrieve
     * @param configDirectory a ply-config directory from which to retrieve the property (i.e., {@literal .ply/config}).
     * @param scope of the property to retrieve
     * @return the property named {@code named} defined within {@code configDirectory} for the given {@code context}
     *         and the given {@code scope} or null if no property exists.
     */
    public static Prop getLocalForceResolution(Context context, String named, File configDirectory, Scope scope) {
        return get(context, named, configDirectory, scope, true, true);
    }

    private static Prop get(Context context, String named, File configDirectory, Scope scope, boolean excludeSystem,
                            boolean forceResolution) {
        Collection<Prop> props = get(context, configDirectory, scope, excludeSystem, forceResolution);
        for (Prop prop : props) {
            if (named.equals(prop.name)) {
                return prop;
            }
        }
        return null;
    }

    /**
     * @param context of the property to retrieve
     * @param named the property name to retrieve
     * @return the property value named {@code named} (defined within {@link PlyUtil#LOCAL_CONFIG_DIR} or
     *         {@link PlyUtil#SYSTEM_CONFIG_DIR}) for the given {@code context} with the current scope or the empty string
     *         if no such property exists
     * @see #getScope
     */
    public static String getValue(Context context, String named) {
        return getValue(context, named, PlyUtil.LOCAL_CONFIG_DIR);
    }

    /**
     * As opposed to {@link #getValue(Context, String)}, this will force the properties
     * to be resolved from the file system and not from environment variables, if any.
     * @param context of the property to retrieve
     * @param named the property name to retrieve
     * @return the property value named {@code named} (defined within {@link PlyUtil#LOCAL_CONFIG_DIR} or
     *         {@link PlyUtil#SYSTEM_CONFIG_DIR}) for the given {@code context} with the current scope or the empty string
     *         if no such property exists
     * @see #getScope
     */
    public static String getValueForceResolution(Context context, String named) {
        return getValueForceResolution(context, named, PlyUtil.LOCAL_CONFIG_DIR);
    }

    /**
     * @param context of the property to retrieve
     * @param named the property name to retrieve
     * @return the property value named {@code named} defined within {@link PlyUtil#LOCAL_CONFIG_DIR} for the given
     *         {@code context} with the current scope or the empty string if no such property exists.
     * @see #getScope
     */
    public static String getLocalValue(Context context, String named) {
        return getLocalValue(context, named, PlyUtil.LOCAL_CONFIG_DIR);
    }

    /**
     * As opposed to {@link #getLocalValue(Context, String)}, this will force the properties
     * to be resolved from the file system and not from environment variables, if any.
     * @param context of the property to retrieve
     * @param named the property name to retrieve
     * @return the property value named {@code named} defined within {@link PlyUtil#LOCAL_CONFIG_DIR} for the given
     *         {@code context} with the current scope or the empty string if no such property exists.
     * @see #getScope
     */
    public static String getLocalValueForceResolution(Context context, String named) {
        return getLocalValueForceResolution(context, named, PlyUtil.LOCAL_CONFIG_DIR);
    }

    /**
     * @param context of the property to retrieve
     * @param named the property name to retrieve
     * @param configDirectory a ply-config directory from which to retrieve the property (i.e., {@literal .ply/config}).
     * @return the property value named {@code named} defined within {@code configDirectory} or
     *         {@link PlyUtil#SYSTEM_CONFIG_DIR} for the given {@code context} and the current scope or the empty string
     *         if no such property exists
     * @see #getScope
     */
    public static String getValue(Context context, String named, File configDirectory) {
        return getValue(context, named, configDirectory, getScope());
    }

    /**
     * As opposed to {@link #getLocal(Context, String, File)}, this will force the properties
     * to be resolved from the file system and not from environment variables, if any.
     * @param context of the property to retrieve
     * @param named the property name to retrieve
     * @param configDirectory a ply-config directory from which to retrieve the property (i.e., {@literal .ply/config}).
     * @return the property value named {@code named} defined within {@code configDirectory} or
     *         {@link PlyUtil#SYSTEM_CONFIG_DIR} for the given {@code context} and the current scope or the empty string
     *         if no such property exists
     * @see #getScope
     */
    public static String getValueForceResolution(Context context, String named, File configDirectory) {
        return getValueForceResolution(context, named, configDirectory, getScope());
    }

    /**
     * @param context of the property to retrieve
     * @param named the property name to retrieve
     * @param configDirectory a ply-config directory from which to retrieve the property (i.e., {@literal .ply/config}).
     * @return the property value named {@code named} defined within {@code configDirectory} for the given {@code context}
     *         and the current scope or the empty string if no such property exists
     * @see #getScope
     */
    public static String getLocalValue(Context context, String named, File configDirectory) {
        return getLocalValue(context, named, configDirectory, getScope());
    }

    /**
     * As opposed to {@link #getLocalValue(Context, String, File)}, this will force the properties
     * to be resolved from the file system and not from environment variables, if any.
     * @param context of the property to retrieve
     * @param named the property name to retrieve
     * @param configDirectory a ply-config directory from which to retrieve the property (i.e., {@literal .ply/config}).
     * @return the property value named {@code named} defined within {@code configDirectory} for the given {@code context}
     *         and the current scope or the empty string if no such property exists
     * @see #getScope
     */
    public static String getLocalValueForceResolution(Context context, String named, File configDirectory) {
        return getLocalValueForceResolution(context, named, configDirectory, getScope());
    }

    /**
     * @param context of the property to retrieve
     * @param named the property name to retrieve
     * @param scope of the property to retrieve
     * @return the property value named {@code named} defined within {@link PlyUtil#LOCAL_CONFIG_DIR} or
     *         {@link PlyUtil#SYSTEM_CONFIG_DIR} for the given {@code context} and the given {@code scope} or the empty
     *         string if no such property exists
     */
    public static String getValue(Context context, String named, Scope scope) {
        return getValue(context, named, PlyUtil.LOCAL_CONFIG_DIR, scope);
    }

    /**
     * As opposed to {@link #getLocal(Context, String, Scope)}, this will force the properties
     * to be resolved from the file system and not from environment variables, if any.
     * @param context of the property to retrieve
     * @param named the property name to retrieve
     * @param scope of the property to retrieve
     * @return the property value named {@code named} defined within {@link PlyUtil#LOCAL_CONFIG_DIR} or
     *         {@link PlyUtil#SYSTEM_CONFIG_DIR} for the given {@code context} and the given {@code scope} or the empty
     *         string if no such property exists
     */
    public static String getValueForceResolution(Context context, String named, Scope scope) {
        return getValueForceResolution(context, named, PlyUtil.LOCAL_CONFIG_DIR, scope);
    }

    /**
     * @param context of the property to retrieve
     * @param named the property name to retrieve
     * @param scope of the property to retrieve
     * @return the property value named {@code named} defined within {@link PlyUtil#LOCAL_CONFIG_DIR} for the given
     *         {@code context} and the given {@code scope} or the empty string if no such property exists
     */
    public static String getLocalValue(Context context, String named, Scope scope) {
        return getLocalValue(context, named, PlyUtil.LOCAL_CONFIG_DIR, scope);
    }

    /**
     * As opposed to {@link #getLocalValue(Context, String, Scope)}, this will force the properties
     * to be resolved from the file system and not from environment variables, if any.
     * @param context of the property to retrieve
     * @param named the property name to retrieve
     * @param scope of the property to retrieve
     * @return the property value named {@code named} defined within {@link PlyUtil#LOCAL_CONFIG_DIR} for the given
     *         {@code context} and the given {@code scope} or the empty string if no such property exists
     */
    public static String getLocalValueForceResolution(Context context, String named, Scope scope) {
        return getLocalValueForceResolution(context, named, PlyUtil.LOCAL_CONFIG_DIR, scope);
    }

    /**
     * @param context of the property to retrieve
     * @param named the property name to retrieve
     * @param configDirectory a ply-config directory from which to retrieve the property (i.e., {@literal .ply/config}).
     * @param scope of the property to retrieve
     * @return the property named {@code named} defined within {@code configDirectory} or
     *         {@link PlyUtil#SYSTEM_CONFIG_DIR} for the given {@code context} and the given {@code scope} or the empty
     *         string if no property exists.
     */
    public static String getValue(Context context, String named, File configDirectory, Scope scope) {
        return getValue(context, named, configDirectory, scope, false, false);
    }

    /**
     * As opposed to {@link #getLocal(Context, String, java.io.File, Scope)}, this will force the properties
     * to be resolved from the file system and not from environment variables, if any.
     * @param context of the property to retrieve
     * @param named the property name to retrieve
     * @param configDirectory a ply-config directory from which to retrieve the property (i.e., {@literal .ply/config}).
     * @param scope of the property to retrieve
     * @return the property named {@code named} defined within {@code configDirectory} or
     *         {@link PlyUtil#SYSTEM_CONFIG_DIR} for the given {@code context} and the given {@code scope} or the empty
     *         string if no property exists.
     */
    public static String getValueForceResolution(Context context, String named, File configDirectory, Scope scope) {
        return getValue(context, named, configDirectory, scope, false, true);
    }

    /**
     * @param context of the property to retrieve
     * @param named the property name to retrieve
     * @param configDirectory a ply-config directory from which to retrieve the property (i.e., {@literal .ply/config}).
     * @param scope of the property to retrieve
     * @return the property value named {@code named} defined within {@code configDirectory} for the given {@code context}
     *         and the given {@code scope} or the empty string if no property exists.
     */
    public static String getLocalValue(Context context, String named, File configDirectory, Scope scope) {
        return getValue(context, named, configDirectory, scope, true, false);
    }

    /**
     * As opposed to {@link #getLocalValue(Context, String, java.io.File, Scope)}, this will force the properties
     * to be resolved from the file system and not from environment variables, if any.
     * @param context of the property to retrieve
     * @param named the property name to retrieve
     * @param configDirectory a ply-config directory from which to retrieve the property (i.e., {@literal .ply/config}).
     * @param scope of the property to retrieve
     * @return the property value named {@code named} defined within {@code configDirectory} for the given {@code context}
     *         and the given {@code scope} or the empty string if no property exists.
     */
    public static String getLocalValueForceResolution(Context context, String named, File configDirectory, Scope scope) {
        return getValue(context, named, configDirectory, scope, true, true);
    }

    private static String getValue(Context context, String named, File configDirectory, Scope scope, boolean excludeSystem,
                                   boolean forceResolution) {
        Prop prop = get(context, named, configDirectory, scope, excludeSystem, forceResolution);
        return (prop == null ? "" : prop.value);
    }

    /**
     * @return the value of the environment variable {@literal ply$scope} or {@link Scope#Default} if the environment
     *         variable is not set.
     */
    public static Scope getScope() {
        String scope = System.getenv("ply$ply.scope"); // cannot use Props as this is called internally while resolving
        return (scope == null ? Scope.Default : new Scope(scope));
    }

}