package net.ocheyedan.ply.ext.props;

import net.ocheyedan.ply.PlyUtil;

import java.io.File;
import java.util.*;

/**
 * User: blangel
 * Date: 12/29/11
 * Time: 5:38 PM
 *
 * Provides access to the properties configured by the user for a given project directory.
 */
public class Props {

    public static Map<Context, Collection<Prop>> get() {
        return get(getScope());
    }

    public static Map<Context, Collection<Prop>> getLocal() {
        return getLocal(getScope());
    }

    public static Map<Context, Collection<Prop>> get(File configDirectory) {
        return get(configDirectory, getScope());
    }

    public static Map<Context, Collection<Prop>> getLocal(File configDirectory) {
        return getLocal(configDirectory, getScope());
    }

    public static Map<Context, Collection<Prop>> get(Scope scope) {
        return get(PlyUtil.LOCAL_CONFIG_DIR, scope);
    }

    public static Map<Context, Collection<Prop>> getLocal(Scope scope) {
        return getLocal(PlyUtil.LOCAL_CONFIG_DIR, scope);
    }

    public static Map<Context, Collection<Prop>> get(File configDirectory, Scope scope) {
        return get(configDirectory, scope, false);
    }

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

    public static Collection<Prop> get(Context context) {
        return get(context, PlyUtil.LOCAL_CONFIG_DIR);
    }

    public static Collection<Prop> getLocal(Context context) {
        return getLocal(context, PlyUtil.LOCAL_CONFIG_DIR);
    }

    public static Collection<Prop> get(Context context, File configDirectory) {
        return get(context, configDirectory, getScope());
    }

    public static Collection<Prop> getLocal(Context context, File configDirectory) {
        return getLocal(context, configDirectory, getScope());
    }

    public static Collection<Prop> get(Context context, Scope scope) {
        return get(context, PlyUtil.LOCAL_CONFIG_DIR, scope);
    }

    public static Collection<Prop> getLocal(Context context, Scope scope) {
        return getLocal(context, PlyUtil.LOCAL_CONFIG_DIR, scope);
    }

    public static Collection<Prop> get(Context context, File configDirectory, Scope scope) {
        return get(context, configDirectory, scope, false);
    }

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
     * @return the value of the environment variable {@literal ply$scope} or {@link Scope#Default} if the environment
     *         variable is not set.
     */
    public static Scope getScope() {
        String scope = System.getProperty("ply$scope");
        return (scope == null ? Scope.Default : new Scope(scope));
    }

}
