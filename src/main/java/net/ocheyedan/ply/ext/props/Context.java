package net.ocheyedan.ply.ext.props;

import java.util.HashMap;
import java.util.Map;

/**
 * User: blangel
 * Date: 12/29/11
 * Time: 5:48 PM
 *
 * Typed representation of a context in {@literal ply}.
 */
public final class Context implements Comparable<Context> {

    private static final Map<String, Context> interned = new HashMap<String, Context>();

    public static Context named(String name) {
        if (interned.containsKey(name)) {
            return interned.get(name);
        }
        Context context = new Context(name);
        interned.put(name, context);
        return context;
    }

    public final String name;

    public Context(String name) {
        this.name = name;
    }

    @Override public String toString() {
        return name;
    }

    @Override public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Context context = (Context) o;
        return (name == null ? context.name == null : name.equals(context.name));
    }

    @Override public int hashCode() {
        return (name == null ? 0 : name.hashCode());
    }

    @Override public int compareTo(Context o) {
        if (o == null) {
            return -1;
        }
        return name.compareTo(o.name);
    }
}
