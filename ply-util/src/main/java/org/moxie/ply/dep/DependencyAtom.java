package org.moxie.ply.dep;

import java.util.concurrent.atomic.AtomicReference;

/**
 * User: blangel
 * Date: 10/2/11
 * Time: 1:31 PM
 *
 * Represents a dependency atom made up of namespace:name:version[:artifactName]
 * If artifactName is null then (name-version.jar) will be used when necessary.
 */
public class DependencyAtom {

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
        return (artifactName == null ? name + "-" + version + ".jar" : artifactName);
    }

    @Override public String toString() {
        return getPropertyName() + ":" + getResolvedPropertyValue();
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
