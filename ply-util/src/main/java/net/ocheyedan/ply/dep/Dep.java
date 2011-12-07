package net.ocheyedan.ply.dep;

import java.util.List;

/**
 * User: blangel
 * Date: 11/4/11
 * Time: 5:05 PM
 *
 * A resolved dependency; includes a {@link DependencyAtom}, the resolved local-repository directory in which
 * the dependency is stored and a list of the dependency's own dependencies.
 */
public final class Dep {

    public final DependencyAtom dependencyAtom;

    public final List<DependencyAtom> dependencies;

    public final String localRepositoryDirectory;

    public Dep(DependencyAtom dependencyAtom, List<DependencyAtom> dependencies, String localRepositoryDirectory) {
        this.dependencyAtom = dependencyAtom;
        this.dependencies = dependencies;
        this.localRepositoryDirectory = localRepositoryDirectory;
    }

    @Override public String toString() {
        return (dependencyAtom == null ? "" : String.format("%s:%s", dependencyAtom.namespace, dependencyAtom.name));
    }

    public String toVersionString() {
        return (dependencyAtom == null ? "" : String.format("%s:%s:%s", dependencyAtom.namespace, dependencyAtom.name, dependencyAtom.version));
    }

    @Override public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Dep dep = (Dep) o;

        return (dependencyAtom == null ? dep.dependencyAtom == null : dependencyAtom.equals(dep.dependencyAtom));
    }

    @Override public int hashCode() {
        return dependencyAtom == null ? 0 : dependencyAtom.hashCode();
    }
}
