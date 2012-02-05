package net.ocheyedan.ply.props;

import java.util.*;

/**
 * User: blangel
 * Date: 2/5/12
 * Time: 3:23 PM
 *
 * Extends {@link java.util.Properties} to provide ordered entry and iteration.
 */
@SuppressWarnings("serial")
public class OrderedProperties extends Properties {

    public OrderedProperties() { }

    public OrderedProperties(OrderedProperties defaults) {
        super(defaults);
    }

    final LinkedHashSet keys = new LinkedHashSet();

    @SuppressWarnings("unchecked")
    @Override public Enumeration<Object> keys() {
        return Collections.enumeration(keys);
    }

    @SuppressWarnings("unchecked")
    @Override public Set<Object> keySet() {
        return Collections.unmodifiableSet(keys);
    }

    @SuppressWarnings("unchecked")
    @Override public Object put(Object key, Object value) {
        keys.add(key);
        return super.put(key, value);
    }

}
