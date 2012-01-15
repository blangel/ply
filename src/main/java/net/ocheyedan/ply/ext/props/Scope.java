package net.ocheyedan.ply.ext.props;

import java.util.HashMap;
import java.util.Map;

/**
 * User: blangel
 * Date: 12/29/11
 * Time: 5:49 PM
 *
 * Typed representation of a scope within {@literal ply}.
 */
public final class Scope {

    public static final Scope Default = new Scope("");

    private static final Map<String, Scope> interned = new HashMap<String, Scope>();

    public static Scope named(String name) {
        if ((name == null) || name.isEmpty()) {
            return Default;
        }
        if (interned.containsKey(name)) {
            return interned.get(name);
        }
        Scope scope = new Scope(name);
        interned.put(name, scope);
        return scope;
    }

    public final String name;

    public Scope(String name) {
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

        Scope scope = (Scope) o;
        return (name == null ? scope.name == null : name.equals(scope.name));
    }

    @Override public int hashCode() {
        return (name == null ? 0 : name.hashCode());
    }
}
