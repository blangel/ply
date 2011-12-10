package net.ocheyedan.ply.input;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

/**
 * User: blangel
 * Date: 11/13/11
 * Time: 9:51 AM
 */
public class UrlResource implements Resource {

    private final String name;

    private final URL url;

    private final AtomicReference<InputStream> ref;

    public UrlResource(String url) throws MalformedURLException {
        this.name = url;
        this.url = new URL(url);
        this.ref = new AtomicReference<InputStream>();
    }

    @Override public String name() {
        return name;
    }

    @Override public InputStream open() throws IOException {
        ref.set(url.openStream());
        return ref.get();
    }

    @Override public Ontology getOntology() {
        return Ontology.Unknown;
    }

    @Override public void close() {
        InputStream stream = ref.get();
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException ioe) {
                throw new AssertionError(ioe);
            }
        }
    }
}
