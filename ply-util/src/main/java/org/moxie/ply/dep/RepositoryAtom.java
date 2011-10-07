package org.moxie.ply.dep;

import org.moxie.ply.Output;

import java.net.URI;

/**
 * User: blangel
 * Date: 10/2/11
 * Time: 1:36 PM
 * 
 * Represents a repository atom made up of repositoryURI[::type].
 * Type is either ply or maven.  If type is null then ply will be used when necessary.
 */
public class RepositoryAtom {

    public static enum Type {
        ply, maven
    }

    public final URI repositoryUri;

    public final Type type;

    public RepositoryAtom(URI repositoryUri, Type type) {
        this.repositoryUri = repositoryUri;
        this.type = type;
    }
    public RepositoryAtom(URI repositoryUri) {
        this(repositoryUri, null);
    }
    public String getPropertyName() {
        return repositoryUri.toString();
    }
    public String getPropertyValue() {
        return (type == null ? "" : type.name());
    }
    public Type getResolvedType() {
        return (type == null ? Type.ply : type);
    }
    public String getResolvedPropertyValue() {
        return getResolvedType().name();
    }
    @Override public String toString() {
        return getPropertyName() + "::" + getResolvedPropertyValue();
    }
    public static RepositoryAtom parse(String atom) {
        if (atom == null) {
            return null;
        }
        String[] resolved = atom.split("::");
        if ((resolved.length < 1) && (resolved.length > 2)) {
            return null;
        }
        URI repositoryUri = URI.create(resolved[0]);
        Type type = null;
        if (resolved.length == 2) {
            if ("ply".equals(resolved[1])) {
                type = Type.ply;
            } else if ("maven".equals(resolved[1])) {
                type = Type.maven;
            } else {
                Output.print("^warn^ unsupported type %s, must be either null, ply or maven.", resolved[1]);
            }
        }
        return new RepositoryAtom(repositoryUri, type);
    }
}
