package net.ocheyedan.ply.props;

import net.ocheyedan.ply.Output;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * User: blangel
 * Date: 10/6/11
 * Time: 12:00 PM
 *
 * Represents a property within the {@literal ply} system.
 */
public final class Prop implements Comparable<Prop> {

    /**
     * Represents the location from which the property value was taken.
     */
    public static enum Loc {
        System, SystemScoped, Local, LocalScoped, AdHoc, AdHocScoped,
        /* Indicated the property was resolved by {@literal ply} and exported as an environment variable */
        Resolved;

        /**
         * @return the corresponding scoped {@link Loc} type.
         */
        Loc scoped() {
            switch (this) {
                case System:
                    return SystemScoped;
                case Local:
                    return LocalScoped;
                case AdHoc:
                    return AdHocScoped;
                default:
                    return this;
            }
        }

        /**
         * @return true if this is a scoped location
         */
        public boolean isScoped() {
            switch (this) {
                case SystemScoped:
                case LocalScoped:
                case AdHocScoped:
                    return true;
                default:
                    return false;
            }
        }
    }

    /**
     * Represents a property value, including its unfiltered version and the {@link Loc} which defined it.
     */
    static class Val {

        final Loc from;

        final String value;

        final String unfiltered;

        public Val(Loc from, String value, String unfiltered) {
            this.from = from;
            this.value = value;
            this.unfiltered = unfiltered;
        }

        Val scoped() {
            return new Val(from.scoped(), value, unfiltered);
        }
    }

    /**
     * Represents a property and all its associated values (those from all defined {@link Loc} objects).
     */
    static class All {

        final Context context;

        final String name;

        private final Map<Scope, Map<Loc, Val>> values;

        public All(Loc type, Scope scope, Context context, String name, String value) {
            this.context = context;
            this.name = name;
            this.values = new HashMap<Scope, Map<Loc, Val>>();
            set(scope, type, value, value);
        }

        public void set(Scope scope, Loc type, String value, String unfiltered) {
            Map<Loc, Val> typeMap = this.values.get(scope);
            if (typeMap == null) {
                typeMap = new HashMap<Loc, Val>();
                this.values.put(scope, typeMap);
            }
            typeMap.put(type, new Val(type, value, unfiltered));
        }

        public void set(Loc type, String value, String unfiltered) {
            for (Scope scope : this.values.keySet()) {
                Map<Loc, Val> typeMap = this.values.get(scope);
                if (typeMap.containsKey(type)) {
                    typeMap.put(type, new Val(type, value, unfiltered));
                }
            }
        }

        public Val get(Scope scope) {
            return get(scope, false);
        }

        Val get(Scope scope, boolean excludeSystem) {
            boolean retrievedFromScoped = !Scope.Default.equals(scope);
            Map<Loc, Val> typeMap = this.values.get(scope);
            if ((typeMap == null) && !Scope.Default.equals(scope)) {
                retrievedFromScoped = false;
                typeMap = this.values.get(Scope.Default);
            }
            if (typeMap == null) {
                return null;
            } else {
                Val value = typeMap.get(Loc.Resolved);
                if (value == null) {
                    value = typeMap.get(Loc.AdHoc);
                    if (value == null) {
                        value = typeMap.get(Loc.Local);
                        if ((value == null) && !excludeSystem) {
                            value = typeMap.get(Loc.System);
                        }
                    }
                }
                return (retrievedFromScoped && (value != null) ? value.scoped(): value);
            }
        }

        public Set<Scope> getScopes() {
            return this.values.keySet();
        }

        @Override public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            All prop = (All) o;
            if (context != null ? !context.equals(prop.context) : prop.context != null) {
                return false;
            }
            return (name == null ? prop.name == null : name.equals(prop.name));
        }

        @Override public int hashCode() {
            int result = context != null ? context.hashCode() : 0;
            result = 31 * result + (name != null ? name.hashCode() : 0);
            return result;
        }

    }

    public final Context context;

    public final String name;

    public final String value;

    public final String unfilteredValue;

    public final Loc type;

    public Prop(Context context, String name, String value, String unfilteredValue, Loc type) {
        this.context = context;
        this.name = name;
        this.value = value;
        this.unfilteredValue = unfilteredValue;
        this.type = type;
    }

    @Override public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Prop prop = (Prop) o;

        if (context != null ? !context.equals(prop.context) : prop.context != null) {
            return false;
        }
        return (name == null ? prop.name == null : name.equals(prop.name));
    }

    @Override public int hashCode() {
        int result = context != null ? context.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    @Override public int compareTo(Prop o) {
        if (o == null) {
            return -1;
        }
        return name.compareTo(o.name);
    }
}
