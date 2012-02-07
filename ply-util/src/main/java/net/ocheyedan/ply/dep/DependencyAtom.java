package net.ocheyedan.ply.dep;

import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: blangel
 * Date: 10/2/11
 * Time: 1:31 PM
 *
 * Represents a dependency atom made up of namespace:name:version[:artifactName][:transient]
 * If {@literal artifactName} is null then (name-version.jar) will be used when necessary.
 * A transient dependency is one which is not packaged and whose dependencies are not themselves
 * resolved for a project which depends upon the transient dependency,
 *
 * To force a different packaging type, explicitly set {@literal artifactName}.
 */
public class DependencyAtom {

    public static final String DEFAULT_PACKAGING = "jar";

    private static final Pattern WHITE_SPACE_REGEX = Pattern.compile("\\s");

    public final String namespace;

    public final String name;

    public final String version;

    public final String artifactName;

    public final boolean transientDep;

    public DependencyAtom(String namespace, String name, String version, String artifactName, boolean transientDep) {
        this.namespace = namespace;
        this.name = name;
        this.version = version;
        this.artifactName = artifactName;
        this.transientDep = transientDep;
    }

    public DependencyAtom(String namespace, String name, String version, String artifactName) {
        this(namespace, name, version, artifactName, false);
    }

    public DependencyAtom(String namespace, String name, String version, boolean transientDep) {
        this(namespace, name, version, null, transientDep);
    }

    public DependencyAtom(String namespace, String name, String version) {
        this(namespace, name, version, null, false);
    }

    public String getPropertyName() {
        return namespace + ":" + name;
    }

    public String getPropertyValue() {
        return getPropertyValueWithoutTransient() + getTransient();
    }

    public String getPropertyValueWithoutTransient() {
        return (version == null ? "" : version) + (artifactName != null ? ":" + artifactName : "");
    }

    public String getResolvedPropertyValue() {
        return version + ":" + getArtifactName() + getTransient();
    }

    public String getArtifactName() {
        return (artifactName == null ? composeArtifact(name, version, null, DEFAULT_PACKAGING) : artifactName);
    }

    private String getTransient() {
        return (transientDep ? ":transient" : "");
    }

    public DependencyAtom with(String packaging) {
        if ((packaging == null) || DEFAULT_PACKAGING.equals(packaging)) {
            return new DependencyAtom(namespace, name, version, transientDep);
        } else if (artifactName == null) {
            return new DependencyAtom(namespace, name, version, composeArtifact(name, version, null, packaging), transientDep);
        } else {
            int index = artifactName.lastIndexOf(".");
            if (index == -1) {
                return new DependencyAtom(namespace, name, version, artifactName + "." + packaging, transientDep);
            } else {
                String artifactWithoutPackaging = artifactName.substring(0, index);
                return new DependencyAtom(namespace, name, version, artifactWithoutPackaging + "." + packaging, transientDep);
            }
        }
    }

    /**
     * Appends {@code classifier} to the artifact name aver the version specification and before the packaging.
     * For instance a dependency of {@literal net.ocheyedan:ply:1.0} (with default jar packaging) normally
     * has an artifact name of {@literal ply-1.0.jar} but with a classifier (say {@literal sources}) would have an
     * artifact name of {@literal ply-1.0-sources.jar}.
     * @param classifier to append to the artifact name after the version but before the packaging
     * @return a {@link DependencyAtom} the same as this one but with an overridden {@link #artifactName} with
     *         the appended {@code classifier}
     */
    public DependencyAtom withClassifier(String classifier) {
        if ((classifier == null) || classifier.isEmpty()) {
            return this;
        }
        Matcher whiteSpaceMatcher = WHITE_SPACE_REGEX.matcher(classifier);
        if (whiteSpaceMatcher.find()) {
            throw new IllegalArgumentException("DependencyAtom objects cannot contain whitespace.");
        }
        return new DependencyAtom(namespace, name, version, composeArtifact(name, version, classifier, getSyntheticPackaging()),
                                  transientDep);
    }

    /**
     * If {@literal this} object has a non-null {@link #artifactName} then this method returns a new {@link DependencyAtom}
     * with the following, composed, {@link #artifactName}: {@link #name} "-" {@link #version} "." {@link #getSyntheticPackaging()}
     * @return a new {@link DependencyAtom} without classifier information or {@literal this} if {@link #artifactName}
     *         is null.
     */
    public DependencyAtom withoutClassifier() {
        if (artifactName == null) {
            return this;
        }
        String packaging = getSyntheticPackaging();
        if (DEFAULT_PACKAGING.equals(packaging)) {
            return new DependencyAtom(namespace, name, version, transientDep);
        } else {
            return new DependencyAtom(namespace, name, version, composeArtifact(name, version, null, packaging), transientDep);
        }
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

    /**
     * Determines if {@code atom} is transient without fulling parsing into a {@link DependencyAtom}.
     * @param atom to parse enough to determine if it represents a transient atom.
     * @return true if {@code atom} would parse to a {@link DependencyAtom} with {@link #transientDep} being true.
     */
    public static boolean isTransient(String atom) {
        return ((atom != null) && atom.endsWith(":transient"));
    }

    /**
     * @param atom from which to strip the transient marker
     * @return {@code atom} without the trailing transient marker if present
     */
    public static String stripTransient(String atom) {
        if (isTransient(atom)) {
            atom = atom.substring(0, atom.length() - ":transient".length());
        }
        return atom;
    }

    public static DependencyAtom parse(String atom, AtomicReference<String> error) {
        if (atom == null) {
            return null;
        }
        atom = atom.trim();
        if (atom.contains(" ")) {
            if (error != null) {
                error.set("has spaces which are not allowed in dependency atoms.");
            }
            return null;
        }
        String[] parsed = atom.split(":");
        if ((parsed.length < 3) || (parsed.length > 5)) {
            if (error != null) {
                if ((parsed.length == 1) && parsed[0].isEmpty()) {
                    parsed = new String[0];
                }
                switch (parsed.length) {
                    case 0: error.set("missing namespace, name and version"); break;
                    case 1: error.set("missing name and version"); break;
                    default: error.set("missing version");
                }
            }
            return null;
        } else if ((parsed.length == 5) && !"transient".equalsIgnoreCase(parsed[4])) {
            error.set("transient");
            return null;
        }
        return (parsed.length == 3
                ? new DependencyAtom(parsed[0], parsed[1], parsed[2])
                : parsed.length == 4
                ? ("transient".equalsIgnoreCase(parsed[3]))
                    ? new DependencyAtom(parsed[0], parsed[1], parsed[2], true)
                    : new DependencyAtom(parsed[0], parsed[1], parsed[2], parsed[3])
                  : new DependencyAtom(parsed[0], parsed[1], parsed[2], parsed[3], "transient".equalsIgnoreCase(parsed[4])));
    }

    private static String composeArtifact(String name, String version, String classifier, String packaging) {
        if (classifier != null) {
            return name + "-" + version + "-" + classifier + "." + packaging;
        } else {
            return name + "-" + version + "." + packaging;
        }
    }

}
