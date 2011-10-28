package org.moxie.ply.submodules;

/**
 * User: blangel
 * Date: 10/27/11
 * Time: 3:58 PM
 */
public class Submodule {

    public final String name;

    public final String dependencyName;

    public Submodule(String name, String dependencyName) {
        this.name = name;
        this.dependencyName = dependencyName;
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

        Submodule submodule = (Submodule) o;

        return (name == null ? submodule.name == null : name.equals(submodule.name));
    }

    @Override public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}
