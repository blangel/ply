package net.ocheyedan.ply.ext.cmd.build;

/**
 * User: blangel
 * Date: 1/2/12
 * Time: 12:41 PM
 */
class Script {

    final String name;

    Script(String name) {
        this.name = name;
    }

    @Override public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Script script = (Script) o;
        return (name == null ? script.name == null : name.equals(script.name));
    }

    @Override public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}
