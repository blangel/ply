package org.moxie.ply.props;

/**
 * User: blangel
 * Date: 10/6/11
 * Time: 12:00 PM
 *
 * A representation of a single property within the ply system including the context from which it came, its scope and
 * whether it is a local override.
 */
public class Prop {

    public final String name;

    public final String value;

    public final String context;

    public final String scope;

    /**
     * Either local, system or a parent name.  This indicates who actually defined this property.
     */
    public final String originator;

    public final Boolean localOverride;

    public Prop(String context, String scope, String name, String value, Boolean localOverride) {
        this.context = context;
        this.scope = scope;
        this.name = name;
        this.value = value;
        this.localOverride = localOverride;
        if (this.localOverride) {
            this.originator = "local";
        } else {
            this.originator = "system";
        }
    }

    public String getContextScope() {
        return (context + (scope.isEmpty() ? "" : "." + scope));
    }
    
}
