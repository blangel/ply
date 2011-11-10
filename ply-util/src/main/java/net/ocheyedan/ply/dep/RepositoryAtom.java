package net.ocheyedan.ply.dep;

import net.ocheyedan.ply.FileUtil;

import java.io.File;
import java.io.IOException;
import java.net.URI;

/**
 * User: blangel
 * Date: 10/2/11
 * Time: 1:36 PM
 * 
 * Represents a repository atom made up of [type:]repositoryURI.
 * Type is either ply or maven.  If type is null then ply will be used when necessary.
 */
public class RepositoryAtom {

    public static enum Type {
        ply, maven
    }

    public static final String MAVEN_REPO_TYPE_PREFIX = "maven:";

    public static final String PLY_REPO_TYPE_PREFIX = "ply:";

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
        return getResolvedPropertyValue() + ":" + getPropertyName();
    }
    public static RepositoryAtom parse(String atom) {
        if (atom == null) {
            return null;
        }
        Type type = null;
        if (atom.startsWith(MAVEN_REPO_TYPE_PREFIX)) {
            type = Type.maven;
            atom = atom.substring(MAVEN_REPO_TYPE_PREFIX.length());
        } else if (atom.startsWith(PLY_REPO_TYPE_PREFIX)) {
            type = Type.ply;
            atom = atom.substring(PLY_REPO_TYPE_PREFIX.length());
        } else {
            type = Type.ply;
        }
        atom = FileUtil.resolveUnixTilde(atom);
        URI repositoryUri;
        // first check if this is a local file reference
        File localRef = new File(atom);
        try {
            localRef = localRef.getCanonicalFile();
            if (localRef.exists()) {
                // don't use toURI as it's not appending 'file://' but simply 'file:'
                atom = "file://" + localRef.getCanonicalPath();
            }
        } catch (IOException ioe) {
            // ignore, try URI directly
        }
        try {
            repositoryUri = URI.create(atom);
        } catch (IllegalArgumentException iae) {
            // invalid, return null
            return null;
        }
        return new RepositoryAtom(repositoryUri, type);
    }
}
