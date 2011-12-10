package net.ocheyedan.ply.input;

import java.io.IOException;
import java.io.InputStream;

/**
 * User: blangel
 * Date: 11/13/11
 * Time: 9:50 AM
 *
 * Represents a consistent way of dealing with {@link InputStream} objects whether they originate from the file system,
 * classpath or over remote protocols like {@literal http}.
 */
public interface Resource {

    /**
     * Represents information known about a resource.  Certain types of resources may not be able to determine
     * their existence until their stream is opened, in which case their {@link Ontology} would be {@link Ontology#Unknown}
     */
    static enum Ontology {
        Unknown, Exists, DoesNotExist
    }

    /**
     * @return a name for this resource
     */
    String name();

    /**
     * @return the actual open resource.
     * @throws IOException if opening the stream results in an exception
     */
    InputStream open() throws IOException;

    /**
     * @return the {@link Ontology} of the {@link Resource}
     */
    Ontology getOntology();

    /**
     * Close the open opened by calling {@link #open()}
     */
    void close();

}
