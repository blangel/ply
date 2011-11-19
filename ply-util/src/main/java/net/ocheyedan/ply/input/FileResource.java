package net.ocheyedan.ply.input;

import net.ocheyedan.ply.FileUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;

/**
 * User: blangel
 * Date: 11/13/11
 * Time: 9:54 AM
 */
public class FileResource implements Resource {

    private final String name;

    private final File file;

    private final AtomicReference<InputStream> ref;

    public FileResource(String file) {
        this.name = file;
        // File doesn't support URI syntax, strip if present
        if (file.startsWith("file://")) {
            file = file.substring(7);
        }
        file = FileUtil.resolveUnixTilde(file);
        this.file = new File(file);
        this.ref = new AtomicReference<InputStream>();
    }

    @Override public String name() {
        return name;
    }

    @Override public InputStream open() throws IOException {
        ref.set(new FileInputStream(file));
        return ref.get();
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
