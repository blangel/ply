package net.ocheyedan.ply.input;

import java.io.IOException;
import java.io.InputStream;

/**
 * User: blangel
 * Date: 11/13/11
 * Time: 9:53 AM
 */
public class ClasspathResource implements Resource {

    private final String name;

    private final InputStream stream;

    public ClasspathResource(String resource, ClassLoader classLoader) {
        this.name = resource;
        ClassLoader loader = (classLoader == null ? (ClasspathResource.class.getClassLoader() == null
                                                        ? ClassLoader.getSystemClassLoader()
                                                        : ClasspathResource.class.getClassLoader())
                                                  : classLoader);
        this.stream = loader.getResourceAsStream(resource);
    }

    @Override public String name() {
        return name;
    }

    @Override public InputStream open() throws IOException {
        return stream;
    }

    @Override public Ontology getOntology() {
        return Ontology.Unknown;
    }

    @Override public void close() {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException ioe) {
                throw new AssertionError(ioe);
            }
        }
    }
}
