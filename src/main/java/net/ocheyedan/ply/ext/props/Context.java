package net.ocheyedan.ply.ext.props;

/**
 * User: blangel
 * Date: 12/29/11
 * Time: 5:48 PM
 *
 * Typed representation of a context in {@literal ply}.
 */
public final class Context implements Comparable<Context> {

    public static Context named(String name) {
        return new Context(name);
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
