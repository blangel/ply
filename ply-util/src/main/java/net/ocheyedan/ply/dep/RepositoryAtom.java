package net.ocheyedan.ply.dep;

import net.ocheyedan.ply.FileUtil;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Comparator;

import static net.ocheyedan.ply.props.PropFile.Prop;

/**
 * User: blangel
 * Date: 10/2/11
 * Time: 1:36 PM
 * 
 * Represents a repository atom made up of [type:]repositoryURI.
 * Type is either ply or maven.  If type is null then ply will be used when necessary.
 */
public class RepositoryAtom {

    /**
     * {@link Comparator} implementation in which local repositories are considered before remote.
     */
    public static final Comparator<RepositoryAtom> LOCAL_COMPARATOR = new Comparator<RepositoryAtom>() {
        @Override public int compare(RepositoryAtom o1, RepositoryAtom o2) {
            boolean local1 = "file".equals(o1.repositoryUri.getScheme());
            boolean local2 = "file".equals(o2.repositoryUri.getScheme());
            if (local1 == local2) {
                return 0;
            } else if (local1) {
                return -1;
            } else {
                return 1;
            }
        }
    };

    public static enum Type {
        ply, maven
    }

    public static final String MAVEN_REPO_TYPE_PREFIX = "maven:";

    public static final String PLY_REPO_TYPE_PREFIX = "ply:";

    public final String preResolvedUri;

    public final URI repositoryUri;

    public final Type type;

    public RepositoryAtom(String preResolvedUri, URI repositoryUri, Type type) {
        this.preResolvedUri = preResolvedUri;
        this.repositoryUri = repositoryUri;
        this.type = type;
    }
    public RepositoryAtom(String preResolvedUri, URI repositoryUri) {
        this(preResolvedUri, repositoryUri, null);
    }
    public String getPropertyName() {
        return repositoryUri.toString();
    }
    public String getPreResolvedUri() {
        return preResolvedUri;
    }
    public String getPropertyValue() {
        return (type == null ? "" : type.name());
    }
    public Type getResolvedType() {
        return (type == null ? Type.ply : type);
    }
    public boolean isPlyType() {
        return (getResolvedType() == Type.ply);
    }
    public String getResolvedPropertyValue() {
        return getResolvedType().name();
    }
    @Override public String toString() {
        return getResolvedPropertyValue() + ":" + getPropertyName();
    }

    @Override public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RepositoryAtom that = (RepositoryAtom) o;

        if (repositoryUri != null ? !repositoryUri.equals(that.repositoryUri) : that.repositoryUri != null) {
            return false;
        }
        return (type == null ? that.type == null : type.equals(that.type));
    }

    @Override public int hashCode() {
        int result = repositoryUri != null ? repositoryUri.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

    public static String atomFromProp(Prop repositoryProp) {
        String value = (!repositoryProp.value().isEmpty()) ? repositoryProp.value() + ":" : "";
        return value + repositoryProp.name;
    }

    public static RepositoryAtom parse(String atom) {
        if ((atom == null) || atom.isEmpty()) {
            return null;
        }
        Type type;
        if (atom.startsWith(MAVEN_REPO_TYPE_PREFIX)) {
            type = Type.maven;
            atom = atom.substring(MAVEN_REPO_TYPE_PREFIX.length());
        } else if (atom.startsWith(PLY_REPO_TYPE_PREFIX)) {
            type = Type.ply;
            atom = atom.substring(PLY_REPO_TYPE_PREFIX.length());
        } else {
            type = Type.ply;
        }
        String preResolved = atom;
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
        return new RepositoryAtom(preResolved, repositoryUri, type);
    }
}
