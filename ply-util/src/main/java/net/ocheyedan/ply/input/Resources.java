package net.ocheyedan.ply.input;

import java.net.MalformedURLException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * User: blangel
 * Date: 11/13/11
 * Time: 9:56 AM
 *
 * Utility in dealing with {@link Resource} objects.
 */
public final class Resources {

    private static final AtomicReference<ClassLoader> LOADER = new AtomicReference<ClassLoader>(Resources.class.getClassLoader());

    public static void setResourcesLoader(ClassLoader classLoader) {
        LOADER.set(classLoader);
    }

    public static Resource parse(String resource) {

        if (resource == null) {
            return null;
        } else if (resource.startsWith("http:")) {
            try {
                return new UrlResource(resource);
            } catch (MalformedURLException murle) {
                throw new RuntimeException(murle);
            }
        } else if (resource.startsWith("classpath:")) {
          return new ClasspathResource(resource.substring(10), LOADER.get());
        } else {
            return new FileResource(resource);
        }

    }

    private Resources() { }
}
