package net.ocheyedan.ply.props;

import net.ocheyedan.ply.FileUtil;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User: blangel
 * Date: 1/15/12
 * Time: 12:49 PM
 *
 * Represents a cache of resolved properties.
 */
final class Cache {

    private static final Map<String, Collection<Prop.All>> _cache = new ConcurrentHashMap<String, Collection<Prop.All>>();
    private static final Map<String, Map<Context, Collection<Prop>>> _contextMapCache = new ConcurrentHashMap<String, Map<Context, Collection<Prop>>>();
    private static final Map<String, Collection<Prop>> _contextCache = new ConcurrentHashMap<String, Collection<Prop>>();

    static boolean contains(File configDirectory, boolean forceResolution) {
        String key = getKey(configDirectory, forceResolution);
        return _cache.containsKey(key);
    }

    static Collection<Prop.All> get(File configDirectory, boolean forceResolution) {
        return _cache.get(getKey(configDirectory, forceResolution));
    }

    static void put(File configDirectory, boolean forceResolution, Collection<Prop.All> props) {
        _cache.put(getKey(configDirectory, forceResolution), props);
    }

    static String getKey(File configDirectory, boolean forceResolution) {
        return FileUtil.getCanonicalPath(configDirectory) + "-" + Boolean.toString(forceResolution);
    }

    static boolean containsContextMap(File configDirectory, Scope scope, boolean excludeSystem, boolean forceResolution) {
        String key = getContextMapKey(configDirectory, scope, excludeSystem, forceResolution);
        return _contextMapCache.containsKey(key);
    }

    static Map<Context, Collection<Prop>> getContextMap(File configDirectory, Scope scope, boolean excludeSystem,
                                                        boolean forceResolution) {
        return _contextMapCache.get(getContextMapKey(configDirectory, scope, excludeSystem, forceResolution));
    }

    static void putContextMap(File configDirectory, Scope scope, boolean excludeSystem, boolean forceResolution,
                              Map<Context, Collection<Prop>> contextMap) {
        _contextMapCache.put(getContextMapKey(configDirectory, scope, excludeSystem, forceResolution), contextMap);
    }

    static String getContextMapKey(File configDirectory, Scope scope, boolean excludeSystem, boolean forceResolution) {
        return getKey(configDirectory, forceResolution) + "-" + scope.name + "-" + Boolean.toString(excludeSystem);
    }

    static boolean containsContext(Context context, File configDirectory, Scope scope, boolean excludeSystem,
                                   boolean forceResolution) {
        String key = getContextKey(context, configDirectory, scope, excludeSystem, forceResolution);
        return _contextCache.containsKey(key);
    }

    static Collection<Prop> getContext(Context context, File configDirectory, Scope scope, boolean excludeSystem,
                                       boolean forceResolution) {
        return _contextCache.get(getContextKey(context, configDirectory, scope, excludeSystem, forceResolution));
    }

    static void putContext(Context context, File configDirectory, Scope scope, boolean excludeSystem,
                           boolean forceResolution, Collection<Prop> props) {
        _contextCache.put(getContextKey(context, configDirectory, scope, excludeSystem, forceResolution), props);
    }

    static String getContextKey(Context context, File configDirectory, Scope scope, boolean excludeSystem,
                                boolean forceResolution) {
        return context.name + "-" + getContextMapKey(configDirectory, scope, excludeSystem, forceResolution);
    }

}
