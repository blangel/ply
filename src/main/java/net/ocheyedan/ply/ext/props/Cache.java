package net.ocheyedan.ply.ext.props;

import net.ocheyedan.ply.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User: blangel
 * Date: 12/29/11
 * Time: 7:03 PM
 *
 * Represents a cache of resolved properties.
 */
final class Cache {

    private static final Map<String, Collection<Prop.All>> _cache = new ConcurrentHashMap<String, Collection<Prop.All>>();

    static boolean contains(File configDirectory) {
        String key = getKey(configDirectory);
        return _cache.containsKey(key);
    }

    static Collection<Prop.All> get(File configDirectory) {
        return _cache.get(getKey(configDirectory));
    }

    static void put(File configDirectory, Collection<Prop.All> props) {
        _cache.put(getKey(configDirectory), props);
    }

    static String getKey(File configDirectory) {
        return FileUtil.getCanonicalPath(configDirectory);
    }

}
