package net.ocheyedan.ply.input;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
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

    private final Map<String, String> headers;

    public UrlResource(String url, Map<String, String> headers) throws MalformedURLException {
        this.name = url;
        this.url = new URL(url);
        this.ref = new AtomicReference<InputStream>();
        this.headers = headers;
    }

    @Override public String name() {
        return name;
    }

    @Override public InputStream open() throws IOException {
        URLConnection urlConnection = url.openConnection();
        if ((headers != null) && (urlConnection instanceof HttpURLConnection)) {
            HttpURLConnection httpURLConnection = (HttpURLConnection) urlConnection;
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                httpURLConnection.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }
        ref.set(urlConnection.getInputStream());
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
