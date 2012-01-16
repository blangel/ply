package net.ocheyedan.ply.props;

import net.ocheyedan.ply.Output;
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
     * @return all properties defined within {@link PlyUtil#LOCAL_CONFIG_DIR} for all contexts with the current scope
     * @see #getScope
     */
    public static Map<Context, Collection<Prop>> getLocal() {
        return getLocal(getScope());
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
     * @param configDirectory a ply-config directory from which to retrieve properties (i.e., {@literal .ply/config}).
     * @return all properties defined within {@code configDirectory} for all contexts with the current scope
     * @see #getScope
     */
    public static Map<Context, Collection<Prop>> getLocal(File configDirectory) {
        return getLocal(configDirectory, getScope());
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
     * @param scope of the properties to retrieve
     * @return all properties defined within {@link PlyUtil#LOCAL_CONFIG_DIR} for all contexts with the given {@code scope}
     */
    public static Map<Context, Collection<Prop>> getLocal(Scope scope) {
        return getLocal(PlyUtil.LOCAL_CONFIG_DIR, scope);
    }

    /**
     * @param configDirectory a ply-config directory from which to retrieve properties (i.e., {@literal .ply/config}).
     * @param scope of the properties to retrieve
     * @return all properties defined within {@code configDirectory} and {@link PlyUtil#SYSTEM_CONFIG_DIR} for all
     *         contexts with the given {@code scope}
     */
    public static Map<Context, Collection<Prop>> get(File configDirectory, Scope scope) {
        return get(configDirectory, scope, false);
    }

    /**
     * @param configDirectory a ply-config directory from which to retrieve properties (i.e., {@literal .ply/config}).
     * @param scope of the properties to retrieve
     * @return  all properties defined within {@code configDirectory} for all contexts with the given {@code scope}
     */
    public static Map<Context, Collection<Prop>> getLocal(File configDirectory, Scope scope) {
        return get(configDirectory, scope, true);
    }

    private static Map<Context, Collection<Prop>> get(File configDirectory, Scope scope, boolean excludeSystem) {
        Collection<Prop.All> props = Loader.get(configDirectory);
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
     * @param context of the properties to retrieve
     * @return all properties defined within {@link PlyUtil#LOCAL_CONFIG_DIR} for the given {@code context} with the
     *         current scope
     * @see #getScope
     */
    public static Collection<Prop> getLocal(Context context) {
        return getLocal(context, PlyUtil.LOCAL_CONFIG_DIR);
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
     * @param context of the properties to retrieve
     * @param configDirectory a ply-config directory from which to retrieve properties (i.e., {@literal .ply/config}).
     * @return all properties defined within {@code configDirectory} for the given {@code context} and the current scope.
     * @see #getScope
     */
    public static Collection<Prop> getLocal(Context context, File configDirectory) {
        return getLocal(context, configDirectory, getScope());
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
     * @param context of the properties to retrieve
     * @param scope of the properties to retrieve
     * @return all properties defined within {@link PlyUtil#LOCAL_CONFIG_DIR} for the given {@code context} and the
     *         given {@code scope}.
     */
    public static Collection<Prop> getLocal(Context context, Scope scope) {
        return getLocal(context, PlyUtil.LOCAL_CONFIG_DIR, scope);
    }

    /**
     * @param context of the properties to retrieve
     * @param configDirectory a ply-config directory from which to retrieve properties (i.e., {@literal .ply/config}).
     * @param scope of the properties to retrieve
     * @return all properties defined within {@code configDirectory} and {@link PlyUtil#SYSTEM_CONFIG_DIR} for the
     *         given {@code context} and the given {@code scope}.
     */
    public static Collection<Prop> get(Context context, File configDirectory, Scope scope) {
        return get(context, configDirectory, scope, false);
    }

    /**
     * @param context of the properties to retrieve
     * @param configDirectory a ply-config directory from which to retrieve properties (i.e., {@literal .ply/config}).
     * @param scope of the properties to retrieve
     * @return all properties defined within {@code configDirectory} for the given {@code context} and the
     *         given {@code scope}.
     */
    public static Collection<Prop> getLocal(Context context, File configDirectory, Scope scope) {
        return get(context, configDirectory, scope, true);
    }

    private static Collection<Prop> get(Context context, File configDirectory, Scope scope, boolean excludeSystem) {
        Collection<Prop.All> props = Loader.get(configDirectory);
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
     * @param context of the property to retrieve
     * @param named the property name to retrieve
     * @param configDirectory a ply-config directory from which to retrieve the property (i.e., {@literal .ply/config}).
     * @param scope of the property to retrieve
     * @return the property named {@code named} defined within {@code configDirectory} or
     *         {@link PlyUtil#SYSTEM_CONFIG_DIR} for the given {@code context} and the given {@code scope} or null if no
     *         property exists.
     */
    public static Prop get(Context context, String named, File configDirectory, Scope scope) {
        return get(context, named, configDirectory, scope, false);
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
        return get(context, named, configDirectory, scope, true);
    }

    private static Prop get(Context context, String named, File configDirectory, Scope scope, boolean excludeSystem) {
        Collection<Prop> props = get(context, configDirectory, scope, excludeSystem);
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
     * @param context of the property to retrieve
     * @param named the property name to retrieve
     * @param configDirectory a ply-config directory from which to retrieve the property (i.e., {@literal .ply/config}).
     * @param scope of the property to retrieve
     * @return the property named {@code named} defined within {@code configDirectory} or
     *         {@link PlyUtil#SYSTEM_CONFIG_DIR} for the given {@code context} and the given {@code scope} or the empty
     *         string if no property exists.
     */
    public static String getValue(Context context, String named, File configDirectory, Scope scope) {
        return getValue(context, named, configDirectory, scope, false);
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
        return getValue(context, named, configDirectory, scope, true);
    }

    private static String getValue(Context context, String named, File configDirectory, Scope scope, boolean excludeSystem) {
        Prop prop = get(context, named, configDirectory, scope, excludeSystem);
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