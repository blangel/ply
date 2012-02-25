package net.ocheyedan.ply;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * User: blangel
 * Date: 2/21/12
 * Time: 9:45 PM
 */
public final class EmptyConcurrentMap<K, V> implements ConcurrentMap<K, V> {

    @SuppressWarnings("unchecked")
    public static <K, V> EmptyConcurrentMap<K, V> get() {
        return (EmptyConcurrentMap<K, V>) EMPTY_CONCURRENT_MAP;
    }
    
    private static final EmptyConcurrentMap EMPTY_CONCURRENT_MAP = new EmptyConcurrentMap();
    
    private final Map<K, V> delegate = Collections.emptyMap();

    private EmptyConcurrentMap() { }
    
    @Override public V putIfAbsent(K key, V value) {
        return null;
    }

    @Override public boolean remove(Object key, Object value) {
        return false;
    }

    @Override public boolean replace(K key, V oldValue, V newValue) {
        return false;
    }

    @Override public V replace(K key, V value) {
        return null;
    }

    @Override public int size() {
        return delegate.size();
    }

    @Override public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override public boolean containsKey(Object key) {
        return delegate.containsKey(key);
    }

    @Override public boolean containsValue(Object value) {
        return delegate.containsValue(value);
    }

    @Override public V get(Object key) {
        return delegate.get(key);
    }

    public V put(K key, V value) {
        return delegate.put(key, value);
    }

    @Override public V remove(Object key) {
        return delegate.remove(key);
    }

    @Override public void putAll(Map<? extends K, ? extends V> m) {
        delegate.putAll(m);
    }

    @Override public void clear() {
        delegate.clear();
    }

    @Override public Set<K> keySet() {
        return delegate.keySet();
    }

    @Override public Collection<V> values() {
        return delegate.values();
    }

    @Override public Set<Entry<K, V>> entrySet() {
        return delegate.entrySet();
    }

    @Override public boolean equals(Object o) {
        return delegate.equals(o);
    }

    @Override public int hashCode() {
        return delegate.hashCode();
    }
}
