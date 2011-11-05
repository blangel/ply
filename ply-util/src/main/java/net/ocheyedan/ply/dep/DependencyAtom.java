package net.ocheyedan.ply.dep;

import java.util.concurrent.atomic.AtomicReference;

/**
 * User: blangel
 * Date: 10/2/11
 * Time: 1:31 PM
 *
 * Represents a dependency atom made up of namespace:name:version[:artifactName]
 * If {@literal artifactName} is null then (name-version.jar) will be used when necessary.
 *
 * To force a different packaging type, explicitly set {@literal artifactName}.
 */
public class DependencyAtom {

    public static final String DEFAULT_PACKAGING = "jar";

    public final String namespace;

    public final String name;

    public final String version;

    public final String artifactName;

    public DependencyAtom(String namespace, String name, String version, String artifactName) {
        this.namespace = namespace;
        this.name = name;
        this.version = version;
        this.artifactName = artifactName;
    }

    public DependencyAtom(String namespace, String name, String version) {
        this(namespace, name, version, null);
    }

    public String getPropertyName() {
        return namespace + ":" + name;
    }

    public String getPropertyValue() {
        return (version == null ? "" : version) + (artifactName != null ? ":" + artifactName : "");
    }

    public String getResolvedPropertyValue() {
        return version + ":" + getArtifactName();
    }

    public String getArtifactName() {
        return (artifactName == null ? name + "-" + version + "." + DEFAULT_PACKAGING : artifactName);
    }

    /**
     * @return the packaging of the dependency (either {@link #DEFAULT_PACKAGING} or the extension of {@link #artifactName}
     *         if {@link #artifactName} is not null).
     */
    public String getSyntheticPackaging() {
        if (artifactName == null) {
            return DEFAULT_PACKAGING;
        } else {
            int index = artifactName.lastIndexOf(".");
            if (index == -1) {
                return DEFAULT_PACKAGING;
            } else {
                return artifactName.substring(index + 1);
            }
        }
    }

    @Override public String toString() {
        return getPropertyName() + ":" + getResolvedPropertyValue();
    }

    @Override public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DependencyAtom that = (DependencyAtom) o;

        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }
        if (namespace != null ? !namespace.equals(that.namespace) : that.namespace != null) {
            return false;
        }
        if (version != null ? !version.equals(that.version) : that.version != null) {
            return false;
        }
        return (artifactName == null ? that.artifactName == null : artifactName.equals(that.artifactName));
    }

    @Override public int hashCode() {
        int result = namespace != null ? namespace.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (version != null ? version.hashCode() : 0);
        result = 31 * result + (artifactName != null ? artifactName.hashCode() : 0);
        return result;
    }

    public static DependencyAtom parse(String atom, AtomicReference<String> error) {
        if (atom == null) {
            return null;
        }
        atom = atom.trim();
        if (atom.contains(" ")) {
            if (error != null) {
                error.set("Spaces not allowed in dependency atom.");
            }
            return null;
        }
        String[] parsed = atom.split(":");
        if ((parsed.length < 3) || (parsed.length > 4)) {
            if (error != null) {
                if ((parsed.length == 1) && parsed[0].isEmpty()) {
                    parsed = new String[0];
                }
                switch (parsed.length) {
                    case 0: error.set("namespace, name and version"); break;
                    case 1: error.set("name and version"); break;
                    default: error.set("version");
                }
            }
            return null;
        }
        return (parsed.length == 3 ? new DependencyAtom(parsed[0], parsed[1], parsed[2]) :
                new DependencyAtom(parsed[0], parsed[1], parsed[2], parsed[3]));
    }

}
