package net.ocheyedan.ply.dep.visitors;

import java.util.HashSet;
import java.util.Set;

/**
 * User: blangel
 * Date: 8/18/14
 * Time: 5:13 PM
 */
class Dependencies {

    private final String self;

    private final Set<String> dependencies;

    public Dependencies(String self) {
        this.self = self;
        this.dependencies = new HashSet<String>();
    }

    public boolean add(String dependency) {
        // filter out JSE classes (note, this is not exhaustive but includes the most common package prefixes
        // starting with Java 6). It is not strictly necessary to filter all it's just an optimization (i.e., things
        // do not break if classes from the JCL are included here).
        if ((dependency.startsWith("java")
                && (dependency.startsWith("java.applet")
                    || dependency.startsWith("java.awt")
                    || dependency.startsWith("java.beans")
                    || dependency.startsWith("java.io")
                    || dependency.startsWith("java.lang")
                    || dependency.startsWith("java.math")
                    || dependency.startsWith("java.net")
                    || dependency.startsWith("java.nio")
                    || dependency.startsWith("java.rmi")
                    || dependency.startsWith("java.security")
                    || dependency.startsWith("java.sql")
                    || dependency.startsWith("java.text")
                    || dependency.startsWith("java.util")
                    || dependency.startsWith("javax.accessibility")
                    || dependency.startsWith("javax.activation")
                    || dependency.startsWith("javax.activity")
                    || dependency.startsWith("javax.annotation")
                    || dependency.startsWith("javax.crypto")
                    || dependency.startsWith("javax.imageio")
                    || dependency.startsWith("javax.jws")
                    || dependency.startsWith("javax.lang")
                    || dependency.startsWith("javax.management")
                    || dependency.startsWith("javax.naming")
                    || dependency.startsWith("javax.net")
                    || dependency.startsWith("javax.print")
                    || dependency.startsWith("javax.rmi")
                    || dependency.startsWith("javax.script")
                    || dependency.startsWith("javax.security")
                    || dependency.startsWith("javax.sound")
                    || dependency.startsWith("javax.sql")
                    || dependency.startsWith("javax.swing")
                    || dependency.startsWith("javax.tools")
                    || dependency.startsWith("javax.xml"))
                // Note, not excluding org.ietf.jgss / org.omg.* / org.w3c.dom.* / org.xml.sax.* as they may collide
                // with other libraries (or would appear to and since it's not an error to exclude but an optimization,
                // going to error on the side of caution).
                )
                // exclude 'sun.' files, compiler will warn about usage and not a common case
                || dependency.startsWith("sun.")
                // no need to reference oneself
                || self.equals(dependency)
                // ignoring generic type references (this is dependent upon how the programmer names her type
                // and so this doesn't always work [ programmer could make generic type more than one character ] but
                // one character is fairly common and so this will eliminate most cases). Not strictly necessary
                // to remove as compilation will still work regardless of this line, this is just an optimization.
                || (dependency.length() == 1)) {
            return false;
        }
        return this.dependencies.add(dependency);
    }

    public Set<String> getDependencies() {
        return dependencies;
    }
}
