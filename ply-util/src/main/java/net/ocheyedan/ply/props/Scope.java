package net.ocheyedan.ply.props;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User: blangel
 * Date: 10/6/11
 * Time: 9:20 AM
 *
 * Properties for scripts within {@literal ply} are identified by their context.  For instance; the compiler script's
 * context is {@literal compiler} and by convention its properties are found within the {@literal compiler.properties}
 * file within the {@literal ply} system's config directory. These context's can also have scopes.  Scopes are
 * variants of the context.  By default they extend from the default scope (in the compiler example all properties
 * defined in the default scope are available, unless overridden, within other scopes).  For instance, the
 * compiler script's properties file with default scope is {@literal compiler.properties} and with scope
 * {@literal test} the properties file is {@literal compiler.test.properties}.  If there are no properties defined
 * within {@literal compiler.test.properties} its properties are all those within {@literal compiler.properties}.  Any
 * additional properties defined within {@literal compiler.test.properties} are supplemental to those within
 * {@literal compiler.properties} and oly available to the {@literal test} scope.  Properties within scopes may
 * override properties in the default scope if the property name is the same.
 *
 * This class is used to provide a scope name in various flavors depending upon the context of use; for instance, if the
 * scope is {@literal test} and it is being printed it should be {@literal ^b^test^r^} but if it is being used
 * as a file suffix (for property file resolution) it should be returned as {@literal .test}.
 */
public final class Scope {

    public static final Scope Default = new Scope("");

    private static final Map<String, Scope> interned = new ConcurrentHashMap<String, Scope>();

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

    public String getFileSuffix() {
        return ((name == null) || name.isEmpty()) ? "" : "." + name;
    }

    public String getPrettyPrint() {
        return ((name == null) || name.isEmpty()) ? "" : "^b^" + name + "^r^ ";
    }

    public String getScriptPrefix() {
        return ((name == null) || name.isEmpty()) ? "" : name + ":";
    }
    
    public String getAdHocSuffix() {
        return ((name == null) || name.isEmpty()) ? "" : "#" + name;
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
