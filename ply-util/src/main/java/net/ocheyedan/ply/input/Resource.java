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
     * @return a name for this resource
     */
    String name();

    /**
     * @return the actual open resource.
     * @throws IOException if opening the stream results in an exception
     */
    InputStream open() throws IOException;

    /**
     * Close the open opened by calling {@link #open()}
     */
    void close();

}
